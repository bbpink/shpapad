import sbt._

class ShpapadProject(info: ProjectInfo) extends AppengineProject(info) with net.stbbs.yasushi.ScalatePlugin {
  val scalate = "org.fusesource.scalate" % "scalate-core" % "1.3"
  val slf4j = "org.slf4j" % "slf4j-jdk14" % "1.6.1"
  val json = "net.debasishg" % "sjson_2.8.0" % "0.8"
}
