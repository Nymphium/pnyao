package com.github.nymphium.pnyao

import scalatags.Text.TypedTag
import scalatags.Text.all._
import com.github.nymphium.pnyao

object Render {
  def infoAsBody(info: Info): TypedTag[String] = {
    div(`class` := "entry", id := info.path)(
      Seq(
        input(`class` := "cell title",
              `type` := "text",
              value := info.title.getOrElse("").toString),
        input(`class` := "cell author",
              `type` := "text",
              value := info.author.getOrElse("").toString),
        span(`class` := "cell tag", info.tag.toString),
        input(`class` := "cell memo",
              `type` := "text",
              value := info.memo.toString),
        span(`class` := "cell path", info.path)
      ))
  }

  def dirnInfoAsBody(d: Files.DirnInfo): TypedTag[String] = {
    div(`class` := "direntry", attr("path") := d._1)(
      h1(d._1),
      div(`class` := "entry label")(
        input(`class` := "cell title",
              `type` := "text",
              value := "title",
              disabled := "disabled"),
        input(`class` := "cell author",
              `type` := "text",
              value := "author",
              disabled := "disabled"),
        span(`class` := "cell tag", "tag"),
        input(`class` := "cell memo",
              `type` := "text",
              value := "memo",
              disabled := "disabled"),
        span(`class` := "cell path", "path")
      ),
      d._2 map infoAsBody
    )
  }

  def render(contents: List[Files.DirnInfo]): TypedTag[String] = {
    html(
      head(
        tag("title")("pnyao"),
        meta(
          content := "text/html; charset=UTF-8"
        ),
        tag("style")(`type` := "text/css")("""
html {
  font-family: "sans serif";
  font-size: 10pt;
}

input {
  background: white;
  color: black;
  font-size: 10pt;
}

.entry {
  display: table-row;
  width: 100%;
}

.cell {
  display: table-cell;
  padding: 3px 10px;
  border: 1px solid #999999;
}

.entry:nth-child(even) {
  background:gray;
  color: white;
}

.entry:nth-child(even) input {
  background: gray;
  color: white;
}

input:hover {
  background: yellow;
}

.entry:nth-child(even) input:hover {
  background: yellow;
  color: black;
}
          """)
      ),
      body(contents map dirnInfoAsBody)
    )
  }
}
