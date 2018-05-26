import io.circe.syntax._
import com.github.nymphium.pnyao._

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
      var absPath = args(0)
      absPath = absPath.replaceAll("~", System.getProperty("user.home"))

      val info: List[Info] = Files.traverseDirectory(absPath)
      // info.map { info =>
        // info.tag += "aaa"
        // info.memo update "OK"
        // println(info.asJson)
      // }

      Files.writeToDB(absPath, info)
      Files.readDB match {
        case Right(l) => println(l)
        case Left(_) => ()
      }

      ()
    } else {
      System.err.println("usage: pnyao DIRECTORY")
    }
  }
}

