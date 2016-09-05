initialCommands in console := """
import akka.actor._
import akka.stream._, scaladsl._
implicit val system = ActorSystem("example")
implicit val mat = ActorMaterializer()

object i {
  def run(fn: String = "tx-genes-01.json") = {
  // val in = io.Source.fromInputStream(new FileInputStream("src/test/resources/tx-genes-01.json"))
    val src = FileIO.fromPath(new java.io.File(s"src/test/resources/${fn}").toPath)

    src.via(JsonFraming.objectScanner(1024)).runForeach(x => println(x.utf8String))
  }

}
"""

cleanupCommands in console := """
mat.shutdown()
system.terminate()
"""
