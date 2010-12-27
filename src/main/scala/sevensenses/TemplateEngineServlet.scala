package sevensenses

import java.io._
import javax.servlet._
import javax.servlet.http._
import org.fusesource.scalate._
import org.fusesource.scalate.servlet._
import sevensenses.model._
import sevensenses.util._

class TemplateEngineServlet extends HttpServlet {

  override def service(req: HttpServletRequest, res: HttpServletResponse) {
    res.setContentType("text/html;charset=utf-8");

    //オペレーション
    val op = req.getRequestURI.replace("/", "") match {
      case "" => "top";
      case _ => req.getRequestURI.replace("/", "")
    }

    try {

      //クラス生成
      val classname = op.head.toUpper + op.tail.toLowerCase
      val obj = Class.forName("sevensenses.model." + classname).getConstructors()(0).newInstance().asInstanceOf[ShpapadView]

      //scalate設定
      val te = new TemplateEngine
      te.resourceLoader = new ServletResourceLoader(getServletContext)
      val context = new ServletRenderContext(te, req, res, getServletContext)

      //httpメソッドで分岐
      req.getMethod match {
        case "GET" =>
          if (obj.redirect != "") {
            res.sendRedirect(obj.redirect)
          } else {
            //render
            context.render("index.ssp", Map("op"->op, "obj"->obj.get(Utility.convParameter(req.getParameterMap.asInstanceOf[java.util.Map[String, Array[String]]]))))
            res.setStatus(HttpServletResponse.SC_OK)
          }
        case "POST" =>
          res.getWriter.println(obj.post(Utility.convParameter(req.getParameterMap.asInstanceOf[java.util.Map[String, Array[String]]]), context))
          res.setStatus(HttpServletResponse.SC_OK)
        case "DELETE" =>
          res.getWriter.println(obj.delete(Utility.convParameter(req.getParameterMap.asInstanceOf[java.util.Map[String, Array[String]]]), context))
          res.setStatus(HttpServletResponse.SC_OK)
        case _ => res.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED)
      }

    } catch {
      case e:ClassNotFoundException => res.sendError(HttpServletResponse.SC_NOT_FOUND)
    }

  }
}
