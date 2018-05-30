package controllers

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

  def foo = Action(parse.json) { request =>
    request.body
      .validate[UpTup]
      .fold(
        errors => {
          BadRequest(
            Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
        },
        uptup => {
          val UpTup(typ, idx, parent, value) = uptup
          pnyao.updateInfo(typ, idx, parent, value)
          Ok(Json.obj("status" -> "OK", "message" -> uptup.toString))
        }
      )
  }
}
