package services

import
    scala.concurrent.Future
  , scala.collection.mutable.Buffer

import javax.inject._

import
    play.api.Logger
  , play.api.inject.ApplicationLifecycle

import
    scalatags.Text.TypedTag
  , scalatags.Text.all._

import
    com.github.nymphium.pnyao.{Files, Info}
  , com.github.nymphium.pnyao.Files.DirnInfo

import
    io.circe._
  , io.circe.syntax._
  , io.circe.generic.auto._

trait PnyaoService {
  def getDB(): Buffer[Files.DirnInfo]
  def updateInfo(`type`: String, idx: Int, parent: String, value: String): Unit
}

@Singleton
class Pnyao @Inject()(lifeCycle: ApplicationLifecycle) extends PnyaoService {
  private var db = (Files.readDB getOrElse Seq()).toBuffer
  private var updated = false
  private var wrote = false
  def getDB() = db
  def addDB(path: String, contents: Seq[Info]): Unit = db = (path, contents) +: db
  def deleteDBEntry(path: String): Unit = {
    val newdb = db.filter {case (path_, _) => path != path_ }
    if (newdb.length < db.length) {
      updated = true
      db = newdb
    }
  }

  // hook to write new data to DB
  private def work() = {
     if (updated && !wrote) {
        db foreach { case (path, contents) =>
          Files.writeToDB(path, contents)
        }
        Logger.info("write to DB")
        wrote = true
      }
  }

  {
    Logger.info("add stop hook ")

    lifeCycle.addStopHook { () => work(); Future.successful(()) }
    sys.addShutdownHook( work )
  }

  def updateInfo(`type`: String,
                 idx: Int,
                 parent: String,
                 value: String): Unit = {

    val dbIdx = db.zipWithIndex.filter { _._1._1 == parent }(0)._2
    val entry = db(dbIdx)._2.toBuffer
    val info = entry(idx)
    var newval: Option[String] = None
    `type` match {
      case "title"  => { info.setTitle(value); newval = info.title }
      case "author" => { info.setAuthor(value); newval = info.author }
      case "memo"   => {
        info.memo.update(value); newval = Some(info.memo.toString)
      }
      case "tag"    => { if(value != "") {info.tag += value; newval = Some(info.tag.toString) }}
      case _        => ()
    }

    newval map { newval =>
      updated = true
      Logger.info(s"update ${`type`} to `${newval}'")
      entry.update(idx, info)
      db.update(dbIdx, (parent, entry.toSeq))
    }
  }
}
