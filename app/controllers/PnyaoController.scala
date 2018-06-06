package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import java.io.File, java.net.URLDecoder,
java.nio.file.{Files => NIOFiles, Paths, StandardCopyOption}

import com.github.nymphium.pnyao.Files
import services.RenderPnyao

import play.api.Logger,
play.api.libs.Files.{TemporaryFile, SingletonTemporaryFileCreator},
play.api.mvc._, play.api.libs.json._, play.api.libs.functional.syntax._,
play.api.inject.ApplicationLifecycle

import javax.inject._

protected case class UpTup(`type`: String,
                           idx: Int,
                           parent: String,
                           value: String,
                           rmTag: Option[Boolean])
protected case class C(value: String)

@Singleton
class PnyaoController @Inject()(
    cc: ControllerComponents,
    lifeCycle: ApplicationLifecycle,
    pnyao: services.Pnyao)(implicit assetsFinder: AssetsFinder)
    extends AbstractController(cc) {

  implicit val reads = (
    (JsPath \ "type").read[String] and
      (JsPath \ "idx").read[Int] and
      (JsPath \ "parent").read[String] and
      (JsPath \ "value").read[String] and
      (JsPath \ "rmTag").readNullable[Boolean]
  )(UpTup.apply(_, _, _, _, _))

  // hook to delete tempfiles {{{
  private var tempList: Seq[TemporaryFile] = Seq()
  private var tempListDeleted = false
  private var hooked = false
  private def deleteTempList() = {
    if (!tempListDeleted) {
      tempList foreach { _.delete }; tempListDeleted = true
    }
  }

  private def setHook() = {
    hooked = true
    Logger.info("PnyaoController/add stop hook")
    lifeCycle.addStopHook { () =>
      {
        deleteTempList()
        Future.successful(())
      }
    }

    sys addShutdownHook deleteTempList
  }
  // }}}

  def index = Action { implicit request =>
    Ok(views.html.index(RenderPnyao.render(pnyao.getDB).toString))
  }

  def updateInfo = Action.async(parse.json) { request =>
    request.body
      .validate[UpTup]
      .fold(
        errors => Future.failed(new Exception(errors.toString)), {
          case UpTup(typ, idx, parent, value, rmTag) => {
            pnyao.updateInfo(typ, idx, parent, value, rmTag)
            Future.successful(
              Ok(
                Json.obj(
                  "status" -> "OK",
                  "message" -> UpTup(typ, idx, parent, value, rmTag).toString)))
          }
        }
      )
  }

  def openPDF(href: String) = Action {
    if (!hooked) setHook

    val it = URLDecoder.decode(href, "UTF-8")
    val fileToServe = SingletonTemporaryFileCreator.create("pnyaotmp", ".pdf")
    tempList = fileToServe +: tempList
    NIOFiles.copy(Paths.get(it),
                  fileToServe.path,
                  StandardCopyOption.REPLACE_EXISTING)
    Logger.info(s"PnyaoController/open ${it}")
    Ok.sendFile(fileToServe.path.toFile)
  }

  def deleteEntry(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")
    val db = pnyao.getDB

    if (db.exists { case (path, _) => path == dpath }) {
      pnyao.deleteDBEntry(dpath)
      Logger.info(s"PnyaoController/delete entry ${dpath}")
      Ok("deleted")
    } else { BadRequest(s"There is no entry ${dpath}") }
  }

  def addEntry(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")
    val file = new File(dpath)

    if (!file.exists || !file.isDirectory) {
      BadRequest(s"${dpath} is not exist");
    } else {
      val db = pnyao.getDB

      if (db.exists { case (path, _) => path == dpath }) {
        Ok(
          Json.obj("status" -> "alreadyloaded",
                   "message" -> s"${dpath} is already loaded"))
      } else {
        val content = Files.traverseDirectory(dpath)

        pnyao.addDB(dpath, content)

        // TODO: response渡してからdbに書き込んだりしたい
        Ok(
          Json.obj(
            "status" -> "OK",
            "message" -> RenderPnyao.dirnInfoAsBody(dpath, content).toString))
      }
    }
  }
}
