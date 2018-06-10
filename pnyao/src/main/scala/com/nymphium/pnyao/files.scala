package com.github.nymphium.pnyao

import java.io.{
  PrintWriter,
  File,
  FileOutputStream,
  IOException,
  ByteArrayOutputStream
}

import java.nio.file.{
  Files => NIOFiles,
  StandardCopyOption,
  StandardOpenOption,
  Paths
}

import org.apache.commons.io.FilenameUtils

import scala.collection.JavaConversions._, scala.io.Source

import com.itextpdf.text.pdf.{PdfReader, PdfWriter, PdfStamper}
import com.itextpdf.text.xml.xmp.XmpWriter

import io.circe._, io.circe.syntax._, io.circe.generic.auto._

object Files {
  // pnyao database with manipulation {{{
  protected var dbPath = "~/.pnyaodb"
  def getDBPath() = dbPath.replaceAll("~", System.getProperty("user.home"))
  def setDBPath(newpath: String) = { dbPath = newpath }
  // }}}

  // tuple (travarsed path, list of info)
  type DirnInfo = (String, Seq[Info])

  // initialize PdfReader
  val _ = { PdfReader.unethicalreading = true }

  // get/set pdf info {{{
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
        tmp.delete
      }
    }
  }

  private def setInfo(info: Info): Unit = {
    val tmp = NIOFiles.createTempFile("pnyaotmp", ".pdf")
    val filepath = info.path

    val out = NIOFiles.newOutputStream(tmp)
    val reader = new PdfReader(filepath)
    val stamper = new PdfStamper(reader, out)
    val rawinfo = reader.getInfo

    info.title.map { rawinfo.put("Title", _) }
    info.author.map { rawinfo.put("Author", _) }
    stamper.setMoreInfo(rawinfo)
    val baos = new ByteArrayOutputStream()
    val xmp = new XmpWriter(baos, rawinfo)
    xmp.close
    stamper.setXmpMetadata(baos.toByteArray())

    stamper.close
    reader.close
    NIOFiles.copy(tmp, Paths.get(filepath), StandardCopyOption.REPLACE_EXISTING)
    out.close
    NIOFiles.delete(tmp)
  }
  // }}}

  /*
   * traverse directory(depth=1);
   * whether the path is absolute or not relies on the caller
   */
  def traverseDirectory(dirName: String): Seq[Info] = {
    val dir = new File(dirName)

    if (dir.exists && dir.isDirectory) {
      dir.listFiles
        .filter(_.isFile)
        .toSeq
        .map(file => getInfoOpt(file.getAbsolutePath))
        .flatten
    } else {
      throw new IOException(s"${dirName} is not a directory or does not exist")
    }
  }

  // get untracked files' information and remove vanished files
  def traverseDirectory(dirName: String, current: Seq[Info]): Seq[Info] = {
    val dir = new File(dirName)
    var current_ = current.toSet

    if (dir.exists && dir.isDirectory) {
      dir.listFiles
        .filter(_.isFile)
        .toSeq
        .map(file => {
          val path = file.getAbsolutePath
          val einfo = current_.find { case Info(_, _, ipath) => path == ipath }
          if (einfo.isEmpty) getInfoOpt(path)
          else {
            current_ = current_ - einfo.get
            if (file.exists) einfo
            else None
          }
        })
        .flatten
    } else {
      throw new IOException(s"${dirName} is not a directory or does not exist")
    }
  }

  // database manipulation
  def writeToDB(targetPath: String, newcontent: Seq[Info]) = {
    val file = new File(getDBPath)
    var changed = false
    val newls = {
      if (!file.exists) {
        file.createNewFile
        changed = true
        Seq((targetPath, newcontent))
      } else {
        readDB match {
          case Right(ls) => {
            changed = true
            (targetPath, newcontent) +: ls.filter(_._1 != targetPath)
          }
          case _ => Seq()
        }
      }
    }

    if (changed) {
      new PrintWriter(file) {
        write(newls.asJson.noSpaces)
        close
      }

      newcontent foreach { info =>
        if (info.isUpdated) setInfo(info)
      }
    }
  }

  def readDB(): Either[io.circe.Error, Seq[DirnInfo]] = {
    val file = new File(getDBPath)

    if (file.exists) {
      parser.decode[Seq[DirnInfo]](Source.fromFile(getDBPath).getLines.mkString)
    } else {
      Left(io.circe.DecodingFailure(s"$getDBPath not found", List()))
    }
  }

  // for conversion from/to JSON {{{
  implicit val encodeDirnInfo: Encoder[DirnInfo] =
    Encoder.forProduct2("path", "contents")(c => (c._1, c._2))

  implicit val decodeDirnInfo: Decoder[DirnInfo] =
    Decoder.forProduct2("path", "contents")(
      Tuple2.apply: (String, Seq[Info]) => DirnInfo)
  // }}}
}
