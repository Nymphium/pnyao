package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import java.io.File, java.net.URLDecoder,
java.nio.file.{Files => NIOFiles, Paths, StandardCopyOption}

import com.github.nymphium.pnyao.Files
import services.RenderPnyao

import
    play.api.Logger
  , play.api.libs.Files.SingletonTemporaryFileCreator
  , play.api.mvc._
  , play.api.libs.json._
  , play.api.libs.functional.syntax._

import javax.inject._

@Singleton
class PnyaoController @Inject()(cc: ControllerComponents, pnyao: services.Pnyao)(implicit assetsFinder: AssetsFinder) extends AbstractController(cc) {
  protected case class UpTup(`type`: String, idx: Int, parent: String, value: String)
  protected case class C(value: String)

  implicit val reads = (
    (JsPath \ "type").read[String] and
    (JsPath \ "idx").read[Int] and
    (JsPath \ "parent").read[String] and
    (JsPath \ "value").read[String]
  )(UpTup.apply(_, _, _, _))

  def index = Action { implicit request =>
    Ok(views.html.index(RenderPnyao.render(pnyao.getDB).toString))
  }

  def updateInfo = Action.async(parse.json) { request =>
    request.body
      .validate[UpTup]
      .fold(
        errors => Future.failed(new Exception(errors.toString)),
        {case UpTup(typ, idx, parent, value) => {
          pnyao.updateInfo(typ, idx, parent, value)
          Future.successful(
            Ok(Json.obj(
              "status" -> "OK",
              "message" -> UpTup(typ, idx, parent, value).toString)))
        }}
      )
  }

  def openPDF(href: String) = Action {
    val it = URLDecoder.decode(href, "UTF-8")
    val fileToServe = SingletonTemporaryFileCreator.create("pnyaotmp", ".pdf")
    NIOFiles.copy(Paths.get(it),
               fileToServe.path,
               StandardCopyOption.REPLACE_EXISTING)
    Logger.info(s"open ${it}")
    Ok.sendFile(
      content = fileToServe.path.toFile,
      onClose = () => { fileToServe.delete }
    )
  }

  def deleteEntry(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")
    val db = pnyao.getDB

    if (db.exists { case (path, _) => path == dpath }) {
      pnyao.deleteDBEntry(dpath)
      Logger.info(s"delete directory ${dpath}")
      Ok("deleted")
    } else { BadRequest(s"There is no entry ${dpath}") }
  }

  def addEntry(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")
    val file = new File(dpath)

    if (! file.exists || ! file.isDirectory) {
      BadRequest(s"${dpath} is not exist");
    } else {
      val db = pnyao.getDB

      if (db.exists {case (path, _) => path == dpath}) {
        Ok(Json.obj("status" -> "alreadyloaded", "message" -> s"${dpath} is already loaded"))
      } else {
        val content = Files.traverseDirectory(dpath)

        pnyao.addDB(dpath, content)

        // TODO: response渡してからdbに書き込んだりしたい
        Ok(Json.obj("status" -> "OK", "message" -> RenderPnyao.dirnInfoAsBody(dpath, content).toString))
      }
    }
  }
}
