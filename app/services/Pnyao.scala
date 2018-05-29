package services

import scalatags.Text.TypedTag
import scalatags.Text.all._
import io.circe._, io.circe.syntax._, io.circe.generic.auto._
import com.github.nymphium.pnyao.{Files, Info}
import com.github.nymphium.pnyao.Files.DirnInfo

object Pnyao {
  private var db = Files.readDB getOrElse List()

  def getDB() = db
  def setDB(items: List[DirnInfo]) = {
    items foreach { case (path, info) => Files.writeToDB(path, info) }
  }

  def foo(aaa: String) = println(aaa)
}
