package com.github.nymphium.pnyao

import io.circe.syntax._

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
      var absPath = args(0)
      absPath = absPath.replaceAll("~", System.getProperty("user.home"))

      val info: List[Pnyao.Info] = Files.traverseDirectory(absPath)
      info.map { info =>
        info.tag += "aaa"
        info.memo update "OK"
        println(info.asJson)
      }

      Files.writeToDB(absPath, info)

      ()
    } else {
      System.err.println("usage: pnyao DIRECTORY")
    }
  }
}
