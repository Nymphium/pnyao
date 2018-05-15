package com.github.nymphium.pnyao

object Main {
  def main(args : Array[String]) : Unit = {
    if (args.length > 0) {
      val info = Pnyao.traverseDirectory(args(0))
      info.map(println)
    } else {
      System.err.println("usage: pnyao DIRECTORY")
    }
  }
}
