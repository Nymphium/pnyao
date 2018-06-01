package controllers

import scala.concurrent.Future

import
    play.api.Logger
  , play.api.mvc._
  , play.api.libs.json._
  , play.api.libs.functional.syntax._

import javax.inject._

@Singleton
class PnyaoController @Inject()(cc: ControllerComponents, pnyao: services.Pnyao) extends AbstractController(cc) {
  protected case class UpTup(`type`: String, idx: Int, parent: String, value: String)

  implicit val reads = (
    (JsPath \ "type").read[String] and
    (JsPath \ "idx").read[Int] and
    (JsPath \ "parent").read[String] and
    (JsPath \ "value").read[String]
  )(UpTup.apply(_, _, _, _))

  def update = Action.async(parse.json) { request =>
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
}
