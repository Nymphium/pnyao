package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import java.io.File, java.net.URLDecoder

import com.github.nymphium.pnyao.Files
import services.RenderPnyao, services.{DBResponse => DR}

import play.api.Logger,
play.api.mvc.{AbstractController, ControllerComponents, Action, AnyContent, Result},
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

  def plain = Action {
    Ok(
      pnyao.getDB
        .fold("") {
          case (z, (path, (contents: Seq[_]))) => {
            "%s\n%s:\n%s".format(z, path, contents.fold("") {
              case (z, x) => "%s\n%s\n-----\n".format(z, x)
            })
          }
        }
        .toString
        .replaceFirst("\n", "")
        .replace("\n$", ""))
  }

  def updateInfo = Action.async(parse.json) { request =>
    Future {
      request.body
        .validate[UpTup]
        .fold(
          errors => InternalServerError(errors.toString),
          { case tup @ UpTup(typ, idx, parent, value, rmTag) => {
            pnyao.updateInfo(typ, idx, parent, value, rmTag)
            Ok(Json.obj("status" -> "OK", "message" -> tup.toString))
          }
        }
      )
    }
  }

  def openPDF(href: String) = Action.async {
    val it = URLDecoder.decode(href, "UTF-8")
    Future.successful(Ok.sendFile(pnyao.openPDF(it)))
  }

  def deleteEntry(path: String) = Action.async {
    val dpath = URLDecoder.decode(path, "UTF-8")
    Future {
      if (pnyao.isSuccessDeleteEntry(dpath)) {
        Logger.info(s"PnyaoController/delete entry ${dpath}")
        Ok("ok")
      } else BadRequest(s"There is no entry ${dpath}")
    }
  }

  def addEntry(path: String) = Action.async {
    val dpath = URLDecoder.decode(path, "UTF-8")

    Future {
      pnyao.addDBEntry(dpath) match {
        case DR.DirNotExist => BadRequest(s"Directory `${dpath}' does not exist")
        case DR.AlreadyLoaded => BadRequest(s"${dpath} is already loaded")
        case DR.OK(res) => {
          Logger.info(s"PnyaoController/add entry `$dpath'")
          Ok("ok")
        }
      }
    }
  }

  def renameFile(src: String, dst: String) = Action {
    val spath = URLDecoder.decode(src, "UTF-8")
    val dpath = URLDecoder.decode(dst, "UTF-8")
    pnyao.syncDBEntries.map { _._2 }.flatten.find { _.path == src } match {
      case Some(info) => {
        pnyao.renameFile(info, dst)
        Logger.info(s"PnyaoController/rename ${src} => ${dst}")
        Ok("ok")
      }
      case None => BadRequest(s"file $src not found")
    }
  }

  def save = Action {
    pnyao.saveToDB

    Ok("saved to DB")
  }
}
