package com.github.nymphium.pnyao

import java.io.{
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  IOException
}
import scala.collection.JavaConversions._
import org.apache.commons.io.{FilenameUtils, IOUtils}
import com.itextpdf.text.pdf.{PdfReader, PdfWriter, PdfStamper}

private object StrUtils {
  private def isMeaningfulString: String => Boolean = {
    case null | "" => false
    case _         => true
  }

  def build(s: String): Option[String] =
    if (isMeaningfulString(s)) Some(s)
    else None
}

object Pnyao {
  // initialize PdfReader
  PdfReader.unethicalreading = true

  private object PdfTag {
    def TITLE = "Title"
    def AUTHOR = "Author"
  }

  // Tag: wrapper of Set[String]; to categorize an info {{{
  protected final class Tag() {
    protected var tags: Set[String] = Set()

    def apply() = tags

    override def toString(): String =
      tags.fold("") { _ + ", " + _ }.replaceFirst(", ", "")

    def +=(tag: String*) = tags ++= tag
    def -=(tag: String) = tags -= tag
    def <-?(tag: String) = tags(tag)
  }
  // }}}

  // memo for info content {{{
  protected final class Memo() {
    protected var memo = ""

    def apply() = memo

    override def toString() = memo

    def update(newmemo: String): Unit = memo = newmemo
  }
  // }}}

  // Info: pdf information data structure {{{
  class Info(var title: Option[String],
             var author: Option[String],
             path: String) {
    val tag = new Tag()
    val memo = new Memo()

    override def toString() =
      s"""title: ${title match {
        case None    => "-"
        case Some(s) => s
      }}
          author: ${author match {
        case None    => "-"
        case Some(s) => s
      }}
          path: ${path}
          tags: ${tag}
          memo: ${memo}"""
        .replaceAll("""\n\s*""", "\n")

    def unapply(): (Option[String], Option[String], String) =
      (title, author, path)

    def setTitle(newtitle: String): Unit = {
      title = StrUtils.build(newtitle)
    }

    def setAuthor(newauthor: String): Unit = {
      author = StrUtils.build(newauthor)
    }
  }
  // }}}

  private def getInfoOpt(filepath: String): Option[Info] = {
    val tmp = File.createTempFile("pnyaotmp", ".pdf")

    if (FilenameUtils.getExtension(filepath).matches("^(?!.*pdf$).*$")) {
      None
    } else {
      try {
        val reader = new PdfReader(filepath)
        val stamper = new PdfStamper(reader, new FileOutputStream(tmp))

        val info = reader.getInfo

        val title = StrUtils.build(info.get(PdfTag.TITLE))
        val creator = StrUtils.build(info.get(PdfTag.AUTHOR))

        stamper.close
        reader.close

        Some(new Info(title, creator, filepath))
      } catch {
        case e: Throwable => {
          System.err.println(filepath, e.getMessage)
          None
        }
      } finally {
        tmp.delete()
      }
    }
  }

  def traverseDirectory(dirName: String): List[Info] = {
    val dir = new File(dirName)

    if (dir.exists && dir.isDirectory) {
      dir.listFiles
        .filter(_.isFile)
        .toList
        .map(file => getInfoOpt(file.getAbsolutePath))
        .flatten
    } else {
      throw new IOException(s"${dirName} is not a directory or not exist")
    }
  }
}
