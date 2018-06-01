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
class AddEntryCounter @Inject()(cc: ControllerComponents, pnyao: services.Pnyao)
    extends AbstractController(cc) {
  def add(path: String) = Action {
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
