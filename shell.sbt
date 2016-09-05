initialCommands in console := """
import akka.actor._
import akka.stream._, scaladsl._
import com.sorrentocorp.akka.stream._

implicit val system = ActorSystem("example")
implicit val mat = ActorMaterializer()

object i {
  def run(fn: String = "tx-genes-01.json") = {
    val src = FileIO.fromPath(new java.io.File(s"src/test/resources/${fn}").toPath)

    src.via(Neo4jRespFraming.scanner).runForeach(x => println(x.utf8String))
  }

}
"""

cleanupCommands in console := """
mat.shutdown()
system.terminate()
"""
