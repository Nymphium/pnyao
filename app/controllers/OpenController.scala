package controllers

import scala.concurrent.Future,
scala.concurrent.ExecutionContext.Implicits.global

import java.io.File, java.net.URLDecoder,
java.nio.file.{Files, Paths, StandardCopyOption}

import play.api.Logger, play.api.libs.Files.SingletonTemporaryFileCreator,
play.api.mvc._, play.api.libs.json.{Json, JsError, __},
play.api.libs.functional.syntax._

import javax.inject._

@Singleton
class OpenController @Inject()(cc: ControllerComponents, pnyao: services.Pnyao)
    extends AbstractController(cc) {
  protected case class C(value: String)

  def open(href: String) = Action {
    val it = URLDecoder.decode(href, "UTF-8")
    val fileToServe = SingletonTemporaryFileCreator.create("pnyaotmp", ".pdf")
    Files.copy(Paths.get(it),
               fileToServe.path,
               StandardCopyOption.REPLACE_EXISTING)
    Logger.info(s"open ${it}")
    Ok.sendFile(
      content = fileToServe.path.toFile,
      onClose = () => { fileToServe.delete }
    )
  }
}

