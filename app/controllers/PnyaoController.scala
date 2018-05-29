package controllers

import
  play.api.mvc._,
  play.api.libs.json._,
  play.api.libs.functional.syntax._

import javax.inject._

class PnyaoController @Inject() extends Controller {
  protected case class UpTup(`type`: String, idx: Int, value: String)

  implicit val reads = (
    (JsPath \ "type").read[String] and
    (JsPath \ "idx").read[Int] and
    (JsPath \ "value").read[String]
  )(UpTup.apply(_, _, _))

  def foo = Action(parse.json) { request =>
    request.body.validate[UpTup].fold(
      errors => {
        BadRequest(
          Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
      },
      uptup => {
        Ok(Json.obj("status" -> "OK", "message" -> uptup.toString))
      }
    )
  }
}
