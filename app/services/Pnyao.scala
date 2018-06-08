package services

import scala.concurrent.Future, scala.collection.mutable.Buffer

import java.io.File

import javax.inject._

import play.api.Logger, play.api.inject.ApplicationLifecycle

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

@Singleton
class Pnyao @Inject()(lifeCycle: ApplicationLifecycle) extends PnyaoService {
  private var db = (Files.readDB getOrElse Seq()).toBuffer
  private var updated = false
  def getDB() = db
  def addDB(path: String, contents: Seq[Info]): Unit = {
    db = (path, contents) +: getDB()
    updated = true
  }

  def deleteDBEntry(path: String): Unit = {
    db = db.filter { case (path_, _) => path != path_ }
    updated = true
  }

  // hook to write new data to DB {{{
  private lazy val work = {
    if (updated) {
      new File(Files.getDBPath).delete

      getDB() foreach {
        case (path, contents) =>
          Logger.info(s"Pnyao/write DB of ${path}")
          Files.writeToDB(path, contents)
      }
      Logger.info("Pnyao/write to DB")
    }
  }

  {
    Logger.info("Pnyao/add stop hook")

    lifeCycle.addStopHook { () => Future.successful(work) }
    sys.addShutdownHook {() => work}
  }
  // }}}

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
}
