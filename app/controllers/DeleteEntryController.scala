package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import javax.inject._
import java.io.File, java.net.URLDecoder,
java.nio.file.{Files => NIOFiles, Paths, StandardCopyOption}

import play.api.Logger, play.api.libs.Files.SingletonTemporaryFileCreator,
play.api.mvc._, play.api.libs.json.{Json, JsError},
play.api.libs.functional.syntax._

import com.github.nymphium.pnyao.Files
import services.RenderPnyao
@Singleton
class DeleteEntryController @Inject()(cc: ControllerComponents,
                                      pnyao: services.Pnyao)
    extends AbstractController(cc) {
  def delete(path: String) = Action {
    val dpath = URLDecoder.decode(path, "UTF-8")
    val db = pnyao.getDB

    if (db.exists { case (path, _) => path == dpath }) {
      pnyao.deleteDBEntry(dpath)
      Logger.info(s"delete directory ${dpath}")
      Ok("deleted")
    } else { BadRequest(s"There is no entry ${dpath}") }
  }
}
