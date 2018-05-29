package com.github.nymphium.pnyao

protected object StrUtils {
  private def isMeaningfulString: String => Boolean = {
    case null | "" => false
    case _         => true
  }

  def build(s: String): Option[String] =
    if (isMeaningfulString(s)) Some(s)
    else None

  def pure: Option[String] => String = {
    case None    => ""
    case Some(s) => s
  }
}
