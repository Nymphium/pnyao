package services

import scala.concurrent.Future, scala.collection.mutable.Buffer

import java.io.File

import javax.inject._

import play.api.Logger, play.api.inject.ApplicationLifecycle,
play.api.libs.Files.{TemporaryFile, SingletonTemporaryFileCreator},
java.nio.file.{Files => NIOFiles, Paths, StandardCopyOption}

import scalatags.Text.TypedTag, scalatags.Text.all._

import com.github.nymphium.pnyao.{Files, Info},
com.github.nymphium.pnyao.Files.DirnInfo

import io.circe._, io.circe.syntax._, io.circe.generic.auto._

trait PnyaoService {
  def getDB(): Buffer[Files.DirnInfo]
  def updateInfo(`type`: String,
                 idx: Int,
                 parent: String,
                 value: String,
                 rmTag: Option[Boolean]): Unit
}

object DBResponse {
  trait DBResponse {}
  case class DirNotExist() extends DBResponse
  case class AlreadyLoaded() extends DBResponse
  case class OK(res: String) extends DBResponse
}

@Singleton
class Pnyao @Inject()(lifeCycle: ApplicationLifecycle) extends PnyaoService {
  private var db = (Files.readDB getOrElse Seq()).toBuffer
  private var updated = false
  private var tempList: Seq[TemporaryFile] = Seq()

  def getDB() = db
  def addDBEntry(path: String) = {
    val file = new File(path)

    if (!(file.exists && file.isDirectory)) {
      DBResponse.DirNotExist
    } else {
      if (db.exists { _._1 == path }) {
        DBResponse.AlreadyLoaded
      } else {
        val contents = Files.traverseDirectory(path)

        db = (path, contents) +: getDB()
        updated = true

        // TODO: response渡してからdbに書き込んだりしたい
        DBResponse.OK(RenderPnyao.dirnInfoAsBody(path, contents).toString)
      }
    }
  }

  def deleteDBEntry(path: String) = {
    if (db.exists { _._1 == path }) {
      db = getDB().filter { _._1 != path }
      updated = true
      true
    } else false
  }

  def syncDBEntries(): Seq[DirnInfo] = {
    db = getDB().map {
      case (path, contents) => (path, Files.traverseDirectory(path, contents))
    }
    db
  }

  def updateInfo(`type`: String,
                 idx: Int,
                 parent: String,
                 value: String,
                 rmTag: Option[Boolean]): Unit = {

    val dbIdx = getDB().zipWithIndex.filter { _._1._1 == parent }(0)._2
    val entry = getDB()(dbIdx)._2.toBuffer
    val info = entry(idx)
    var newval: Option[String] = None
    `type` match {
      case "title"  => { info.setTitle(value); newval = info.title }
      case "author" => { info.setAuthor(value); newval = info.author }
      case "memo" => {
        info.memo.update(value); newval = Some(info.memo.toString)
      }
      case "tag" =>
        if (value != "")
          rmTag match {
            case Some(rmt) => {
              if (rmt) {
                info.tag -= value
              } else {
                info.tag += value
              }

              newval = Some(info.tag.toString)
            }

            case None => throw new Exception("tag removeflag is not set")
          }

      case _ => ()
    }

    newval map { newval =>
      updated = true
      Logger.info(s"Pnyao/update ${`type`} to `${newval}'")
      entry.update(idx, info)
      getDB().update(dbIdx, (parent, entry.toSeq))
    }
  }

  def openPDF(it: String) = {
    val fileToServe = SingletonTemporaryFileCreator.create("pnyaotmp", ".pdf")
    tempList = fileToServe +: tempList
    NIOFiles.copy(Paths.get(it),
                  fileToServe.path,
                  StandardCopyOption.REPLACE_EXISTING)
    Logger.info(s"PnyaoController/open ${it}")
    fileToServe.path.toFile
  }

  def saveToDB() = save()

  private def save() = {
    if (updated) {
      new File(Files.getDBPath).delete

      getDB() foreach {
        case (path, contents) =>
          Logger.info(s"Pnyao/write DB of ${path}")
          Files.writeToDB(path, contents)
      }

      Logger.info("Pnyao/write to DB")
    }

    updated = false
  }

  // hook to write new data to DB {{{
  private lazy val work = {
    save()

    tempList foreach { _.delete }
    Logger.info("Pnyao/delete tempfiles")
  }

  {
    Logger.info("Pnyao/add stop hook")

    lifeCycle.addStopHook { () =>
      Future.successful(work)
    }
    sys.addShutdownHook { () =>
      work
    }
  }
  // }}}
}
