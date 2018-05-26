package com.github.nymphium.pnyao

import java.io.{
  PrintWriter,
  File,
  FileInputStream,
  FileOutputStream,
  IOException
}

import scala.collection.JavaConversions._
import scala.io.Source
import org.apache.commons.io.{FilenameUtils, IOUtils}
import com.itextpdf.text.pdf.{PdfReader, PdfWriter, PdfStamper}
import io.circe._, io.circe.syntax._, io.circe.generic.auto._


object Files {
  protected var dbPath = "~/.pnyaodb"
  def getDBPath() = dbPath.replaceAll("~", System.getProperty("user.home"))
  def setDBPath(newpath: String) = { dbPath = newpath }

  case class DirnInfo(dir : String, infos : List[Pnyao.Info])

  // initialize PdfReader
  PdfReader.unethicalreading = true

  private object PdfTag {
    def TITLE = "Title"
    def AUTHOR = "Author"
  }

  private def getInfoOpt(filepath: String): Option[Pnyao.Info] = {
    val tmp = File.createTempFile("pnyaotmp", ".pdf")

    if (FilenameUtils.getExtension(filepath).matches("^(?!.*pdf$).*$")) {
      None
    } else {
      try {
        val reader = new PdfReader(filepath)
        val stamper = new PdfStamper(reader, new FileOutputStream(tmp))

        val info = reader.getInfo

        val title = Common.StrUtils.build(info.get(PdfTag.TITLE))
        val creator = Common.StrUtils.build(info.get(PdfTag.AUTHOR))

        stamper.close
        reader.close

        Some(new Pnyao.Info(title, creator, filepath))
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

  def traverseDirectory(dirName: String): List[Pnyao.Info] = {
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

  // TODO: read db and update
  def writeToDB(targetPath : String, infos : List[Pnyao.Info]) = {
    new PrintWriter(getDBPath) {
      val obj = DirnInfo(targetPath, infos)
      write(obj.asJson.noSpaces)
      close
    }
  }

  def readDB() : Either[io.circe.Error, DirnInfo] = {
    val db = getDBPath
    val dbcontent = Source.fromFile(db).getLines.mkString
    parser.decode[DirnInfo](dbcontent)
  }

  implicit val encodeDirnInfo: Encoder[DirnInfo] =
    Encoder.forProduct2("path", "contents")(c => (c.dir, c.infos))

  implicit val decodeDirnInfo: Decoder[DirnInfo] =
    Decoder.forProduct2("path", "contents")(DirnInfo.apply)
}

