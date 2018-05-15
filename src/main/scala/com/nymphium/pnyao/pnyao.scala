package com.github.nymphium.pnyao

import java.io.{File, FileInputStream, InputStream,IOException}
import scala.collection.JavaConversions._
import org.apache.commons.io.{FileUtils, FilenameUtils, IOUtils}
import org.apache.tika.exception._
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.PDF
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.pdf.PDFParser
import org.apache.tika.sax.WriteOutContentHandler

private object Misc {
  def isMeaningfulString : String => Boolean =
    _ match {
      case null | "" => false
      case _ => true
    }

  def buildOptionalString(s : String) : Option[String] =
    if (isMeaningfulString(s)) Some(s)
    else None
}

object Pnyao {
  // Info: pdf information data structure {{{
  sealed abstract case class Info(title : Option[String], author : Option[String], path : String) {
    override def toString() =
      s"""title: ${title match {case None => "-" case Some(s) => s}}
          author: ${author match {case None => "-" case Some(s) => s}}
          path: ${path}""".replaceAll("""\n\s*""", "\n")
  }

  object Info {
    def apply(rawtitle : String, rawauthor : String, path : String) : Info =
      new Info(Misc.buildOptionalString(rawtitle), Misc.buildOptionalString(rawauthor), path){}

  }
  // }}}


  def getFileInfoOpt(file : File) : Option[Info] = {
    val parser = new PDFParser()
    val ctx = new ParseContext()
    val handler = new WriteOutContentHandler(-1)
    val metadata = new Metadata()

    var iostream : InputStream = null
    val filepath = file.getAbsolutePath()
    if (FilenameUtils.getExtension(filepath).matches("^(?!.*pdf$).*$")) {
      None
    } else {
      try {
        iostream = new FileInputStream(file)
        parser.parse(iostream, handler, metadata, ctx)
        val title = metadata.get(PDF.DOC_INFO_TITLE)
        val creator = metadata.get(PDF.DOC_INFO_CREATOR)
        Some(Info(title, creator, filepath))
      } catch {
        case e : Throwable => {
          System.err.println(filepath, e.getMessage())
          None
        }
      } finally {
        IOUtils.closeQuietly(iostream)
      }
    }
  }

  def traverseDirectory(dirName : String) : List[Info] = {
    val dirName_ = dirName.replaceFirst("^~",System.getProperty("user.home"))
    val parser = new PDFParser()
    val ctx = new ParseContext()
    val handler = new WriteOutContentHandler(-1)
    val metadata = new Metadata()
    val dir = new File(dirName_)

    if (dir.exists && dir.isDirectory) {
      dir.listFiles.filter(_.isFile)
        .toList.map(file => getFileInfoOpt(file))
        .flatten
    } else {
      throw new IOException(s"${dirName} is not directory or not exist")
    }
  }
}

