package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import java.io.File, java.net.URLDecoder

import com.github.nymphium.pnyao.Files
import services.RenderPnyao, services.{DBResponse => DR}

import play.api.Logger,
play.api.mvc.{AbstractController, ControllerComponents, Action},
play.api.libs.json.{JsPath, Json}, play.api.libs.functional.syntax._,
play.api.inject.ApplicationLifecycle

import javax.inject.{Singleton, Inject}

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

  def index = Action { implicit request =>
    Ok(views.html.index(RenderPnyao.render(pnyao.syncDBEntries).toString))
  }

  def updateInfo = Action.async(parse.json) { request =>
    request.body
      .validate[UpTup]
      .fold(
        errors => Future.failed(new Exception(errors.toString)), {
          case tup @ UpTup(typ, idx, parent, value, rmTag) => {
            pnyao.updateInfo(typ, idx, parent, value, rmTag)
            Future.successful(
              Ok(Json.obj("status" -> "OK", "message" -> tup.toString)))
          }
        }
      )
  }

  def openPDF(href: String) = Action.async {
    val it = URLDecoder.decode(href, "UTF-8")
    Future.successful(Ok.sendFile(pnyao.openPDF(it)))
  }

  def deleteEntry(path: String) = Action.async {
    val dpath = URLDecoder.decode(path, "UTF-8")
    if (pnyao.deleteDBEntry(dpath)) {
      Logger.info(s"PnyaoController/delete entry ${dpath}")
      Future.successful(Ok("deleted"))
    } else Future.failed(new Exception(s"There is no entry ${dpath}"))
  }

  def addEntry(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")

    pnyao.addDBEntry(dpath) match {
      case DR.DirNotExist => BadRequest(s"Directory `${dpath}' does not exist")
      case DR.AlreadyLoaded =>
        Ok(
          Json.obj("status" -> "alreadyloaded",
                   "message" -> s"${dpath} is already loaded"))
      case DR.OK(res) => {
        Logger.info(s"PnyaoController/add entry `$dpath'")
        Ok(Json.obj("status" -> "OK", "message" -> res))
      }
    }
  }
}
