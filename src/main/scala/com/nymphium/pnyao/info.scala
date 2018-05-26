package com.github.nymphium.pnyao

import io.circe._, io.circe.syntax._, io.circe.generic.auto._,
io.circe.generic.semiauto._

// Tag: wrapper of Set[String]; to categorize an info {{{
protected final class Tag() {
  protected var tags: Set[String] = Set()

  def apply() = tags

  override def toString(): String =
    tags.fold("") { _ + ", " + _ }.replaceFirst(", ", "")

  def +=(tag: String*) = tags ++= tag
  def -=(tag: String) = tags -= tag
  def <-?(tag: String) = tags(tag)
}
// }}}

// memo for info content {{{
protected final class Memo() {
  protected var memo = ""

  def apply() = memo

  override def toString() = memo

  def update(newmemo: String): Unit = memo = newmemo
}
// }}}

// Info: pdf information data structure
class Info(var title: Option[String],
           var author: Option[String],
           val path: String) {
  val tag = new Tag()
  val memo = new Memo()

  override def toString() =
    s"""title: ${title match {
      case None    => "-"
      case Some(s) => s
    }}
      author: ${author match {
      case None    => "-"
      case Some(s) => s
    }}
      path: ${path}
      tags: ${tag}
      memo: ${memo}"""
      .replaceAll("""\n\s*""", "\n")

  def unapply(): (Option[String], Option[String], String) =
    (title, author, path)

  def setTitle(newtitle: String): Unit = {
    title = StrUtils.build(newtitle)
  }

  def setAuthor(newauthor: String): Unit = {
    author = StrUtils.build(newauthor)
  }
}

object Info {
  def apply(title: String, author: String, path: String) =
    new Info(StrUtils.build(title), StrUtils.build(author), path)

  // for conversion from/to JSON   {{{
  implicit val encodeString: Encoder[String] = {
    case null => Json.fromString("")
    case s    => Json.fromString(s)
  }

  implicit val encodeTag: Encoder[Tag] = new Encoder[Tag] {
    final def apply(t: Tag) = Json.fromString(t.toString)
  }

  implicit val encodeMemo: Encoder[Memo] = new Encoder[Memo] {
    final def apply(m: Memo) = Json.fromString(m.toString)
  }

  implicit val encodeInfo: Encoder[Info] =
    Encoder.forProduct5("title", "author", "path", "tag", "memo")(
      ii =>
        (StrUtils.pure(ii.title),
         StrUtils.pure(ii.author),
         ii.path,
         ii.tag,
         ii.memo))

  implicit val decodeInfo: Decoder[Info] =
    Decoder.forProduct5("title", "author", "path", "tag", "memo")({
      case (title, author, path, tag, memo) =>
        val info = new Info(StrUtils.build(title), StrUtils.build(author), path)
        info.tag += tag
        info.memo update memo
        info
    }: (String, String, String, String, String) => Info)
//   }}}
}
