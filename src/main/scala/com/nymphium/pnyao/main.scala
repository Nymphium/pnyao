import io.circe.syntax._
import com.github.nymphium.pnyao._

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
      Files.readDB match {
        case Right(l) => println(Render.render(l))
        case Left(_) => {
          var absPath = args(0)
          absPath = absPath.replaceAll("~", System.getProperty("user.home"))

          val info: List[Info] = Files.traverseDirectory(absPath)
          Files.writeToDB(absPath, info)
        }
      }
    } else {
      System.err.println("usage: pnyao DIRECTORY")
    }
  }
}
