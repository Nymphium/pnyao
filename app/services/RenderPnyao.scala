package services

import
    com.github.nymphium.pnyao.{Info, Files}
  , com.github.nymphium.pnyao.Files.DirnInfo

import
    io.circe._
  , io.circe.syntax._
  , io.circe.generic.auto._

import
    scalatags.Text.TypedTag
  , scalatags.Text.all._

object RenderPnyao {
  def infoAsBody(idx: Int, info: Info): TypedTag[String] = {
    div(`class` := "entry", id := info.path)(
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
        span(`class` := "tag", info.tag.toString),
        input(`class` := "memo",
              attr("idx") := idx,
              `type` := "text",
              value := info.memo.toString),
        span(`class` := "path", info.path)
      ))
  }

  def dirnInfoAsBody(d: Files.DirnInfo): TypedTag[String] = {
    div(`class` := "direntry", attr("path") := d._1)(
      h1(d._1),
      div(`class` := "entry label")(
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
  }

  def render(contents: List[Files.DirnInfo]): TypedTag[String] = {
    div(`class` := "renderedBody")(
      contents.map(dirnInfoAsBody)
    )
  }
}
