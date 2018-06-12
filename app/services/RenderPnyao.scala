package services

import java.net.URLEncoder, java.nio.file.Paths

import com.github.nymphium.pnyao.{Info, Files},
com.github.nymphium.pnyao.Files.DirnInfo

import io.circe._, io.circe.syntax._, io.circe.generic.auto._

import play.api.Logger

import scalatags.Text.TypedTag, scalatags.Text.all._

object RenderPnyao {
  def infoAsBody(idx: Int, info: Info): TypedTag[String] = {
    val path = Paths.get(info.path)

    div(`class` := "info", attr("path") := info.path, style:= "display: block;")(
      Seq(
        span(`class` := "index", idx),
        input(`class` := "title",
              attr("idx") := idx,
              `type` := "text",
              value := info.title.getOrElse("").toString),
        input(`class` := "author",
              attr("idx") := idx,
              `type` := "text",
              value := info.author.getOrElse("").toString),
        span(`class` := "tag")(
          span(`class`:="tag0 add", "+")
          ,
          info.tag().map {c => span(`class`:="tag0 t", c) }.toSeq
          ),
        textarea(`class` := "memo",
              attr("idx") := idx,
              cols:="100",
              rows:="100",
              )(info.memo.toString),
        // avoid Play to decode
        a(
          `class` := "path",
          href := s"/open/${URLEncoder
            .encode(URLEncoder.encode(path.toString, "UTF-8"), "UTF-8")}",
          target := "_blank",
          path.getFileName.toString
        )
      ))
  }

  def dirnInfoAsBody(d: Files.DirnInfo): TypedTag[String] = {
    div(`class` := "direntry", attr("path") := d._1, attr("fold") := "false")(
      div(`class` := "entryLabel")(
        span(`class` := "label", d._1),
        button(`type` := "button",
               attr("path") := d._1,
               `class` := "delete",
               "delete from DB")
      ),
      div(`class` := "content")(
        div(`class` := "infoLabel")(
          span(`class` := "index", "index"),
          input(`class` := "title",
                `type` := "text",
                value := "title",
                disabled := "disabled"),
          input(`class` := "author",
                `type` := "text",
                value := "author",
                disabled := "disabled"),
          span(`class` := "tag", "tag"),
          input(`class` := "memo",
                `type` := "text",
                value := "memo",
                disabled := "disabled"),
          span(`class` := "path", "path")
        ),
        d._2.zipWithIndex.map { case (info, idx) => infoAsBody(idx, info) }
      )
    )
  }

  def render(contents: Seq[Files.DirnInfo]): TypedTag[String] = {
    div(`id` := "renderField")(
      contents.map(dirnInfoAsBody)
    )
  }
}
