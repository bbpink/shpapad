package sevensenses.util

import java.util._
import scala.collection.JavaConverters._

case class Utility()

object Utility {

  def convParameter(m:Map[String, Array[String]]):scala.collection.immutable.Map[String, scala.collection.immutable.List[String]] = {
    var res = scala.collection.mutable.Map.empty[String, scala.collection.immutable.List[String]]

    m.asScala foreach { case(k, v) =>
      var s = scala.collection.immutable.List.empty[String]
      v foreach { params =>
        s = s ::: scala.collection.immutable.List(params)
      }

      res(k) = s
    }

    res.toMap
  }

  //文字列中にurlがある場合aタグで囲って返す
  def wrapWithAnchor(str:String):String = {
    val urlr = """http://[\w\-\./%\?=#]+""".r
    def wrapping(s:String):String = urlr.findFirstMatchIn(s) match {
      case None => s
      case Some(m) =>
        m.before.toString + "<a href=\"" + m.matched.toString + "\">" + m.matched.toString + "</a>" + wrapping(m.after.toString)
    }

    wrapping(str)
  }

}
