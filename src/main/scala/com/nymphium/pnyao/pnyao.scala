package com.github.nymphium.pnyao

import java.io.{File, FileInputStream, FileWriter, InputStream, PrintWriter,IOException}
import scala.collection.JavaConversions._
import org.apache.commons.io.{FileUtils, FilenameUtils, IOUtils}
import org.apache.tika.exception._
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.PDF
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.pdf.PDFParser
import org.apache.tika.sax.WriteOutContentHandler

object Pnyao {
  case class Info(title : Option[String], author : Option[String], path : String) {
    override def toString() = {
      val title_ = title match {case None => "-" case Some(s) => s}
      val author_ = author match {case None => "-" case Some(s) => s}
      s"""title: ${title_}
author: ${author_}
path: ${path}"""
    }
  }

  def isMeaningfulString : String => Boolean =
    _ match {
      case null => false
      case "" => false
      case _ => true
    }

  def nullableToString : String => String =
    _ match {
      case null => ""
      case s => s
    }

  def buildOptionalString(s : String) : Option[String] =
    if (isMeaningfulString(s)) {
      Some(s)
    } else None

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
        val _ = parser.parse(iostream, handler, metadata, ctx)

        val title = metadata.get(PDF.DOC_INFO_TITLE)
        val creator = metadata.get(PDF.DOC_INFO_CREATOR)

        Some(Info(buildOptionalString(title), buildOptionalString(creator), filepath))
      } catch {
        case e : TikaException => {
          println(filepath, e.getMessage())
          None
        }
        case e : IOException => {
          println(filepath, e.getMessage())
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
      val files = dir.listFiles.filter(_.isFile).toList

      files.map(file => getFileInfoOpt(file)).flatten
    } else {
      throw new IOException(s"${dirName} is not directory or not exist")
    }
  }
}
