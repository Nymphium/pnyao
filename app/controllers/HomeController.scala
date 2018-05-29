package controllers

import
    play.api.http.{ContentTypeOf, ContentTypes, Writeable}
  , play.api.mvc._

import javax.inject._

import services._

import io.circe.syntax._
import scalatags.Text.all._
import com.github.nymphium.pnyao._

@Singleton
class HomeController @Inject()(cc: ControllerComponents)(
    implicit assetsFinder: AssetsFinder)
    extends AbstractController(cc) {
  def index = Action { implicit request =>
    Ok(views.html.index(RenderPnyao.render(Pnyao.getDB).toString))
  }
}
