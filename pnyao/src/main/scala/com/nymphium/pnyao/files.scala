package com.github.nymphium.pnyao

import java.io.{
  PrintWriter,
  File,
  FileOutputStream,
  IOException
}

import org.apache.commons.io.FilenameUtils

import
    scala.collection.JavaConversions._
  , scala.io.Source

import com.itextpdf.text.pdf.{PdfReader, PdfWriter, PdfStamper}

import
    io.circe._
   , io.circe.syntax._
   , io.circe.generic.auto._

object Files {
  // pnyao database with manipulation {{{
  protected var dbPath = "~/.pnyaodb"
  def getDBPath() = dbPath.replaceAll("~", System.getProperty("user.home"))
  def setDBPath(newpath: String) = { dbPath = newpath }
  // }}}

  // tuple (travarsed path, list of info)
  type DirnInfo = (String, List[Info])

  // initialize PdfReader
  val _ = { PdfReader.unethicalreading = true }

  private def getInfoOpt(filepath: String): Option[Info] = {
    val tmp = File.createTempFile("pnyaotmp", ".pdf")

    if (FilenameUtils.getExtension(filepath).matches("^(?!.*pdf$).*$")) {
      None
    } else {
      try {
        val reader = new PdfReader(filepath)
        val stamper = new PdfStamper(reader, new FileOutputStream(tmp))
        val rawinfo = reader.getInfo

        stamper.close
        reader.close

        Some(Info(rawinfo.get("Title"), rawinfo.get("Author"), filepath))
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

  /*
   * traverse directory(depth=1);
   * whether the path is absolute or not relies on the caller
   */
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

  // database manipulation {{{
  def writeToDB(targetPath: String, newcontent: List[Info]) = {
    val file = new File(getDBPath)
    var changed = false
    val newls = {
      if (!file.exists) {
        file.createNewFile
        changed = true
        List((targetPath, newcontent))
      } else {
        readDB match {
          case Right(ls) => {
            changed = true
            (targetPath, newcontent) :: ls.filter(_._1 != targetPath)
          }
          case _ => List()
        }
      }
    }

    if (changed) {
      new PrintWriter(file) {
        write(newls.asJson.noSpaces)
        close
      }
    }
  }

  def readDB(): Either[io.circe.Error, List[DirnInfo]] = {
    val file = new File(getDBPath)

    parser.decode[List[DirnInfo]] {
      if (file.exists) {
        Source.fromFile(getDBPath).getLines.mkString
      } else {
        ""
      }
    }
  }

  // for conversion from/to JSON   {{{
  implicit val encodeDirnInfo: Encoder[DirnInfo] =
    Encoder.forProduct2("path", "contents")(c => (c._1, c._2))

  implicit val decodeDirnInfo: Decoder[DirnInfo] =
    Decoder.forProduct2("path", "contents")(
      Tuple2.apply: (String, List[Info]) => DirnInfo)
  //   }}}
  // }}}
}
