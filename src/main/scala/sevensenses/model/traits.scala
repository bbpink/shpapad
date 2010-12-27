package sevensenses.model

import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.{FilterOperator, SortDirection }
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}
import org.fusesource.scalate._
import org.fusesource.scalate.servlet._

abstract class ShpapadView {
  var redirect = ""
  var username = ""
  var logoutable = true
  var menu:Map[String, String] = Map()

  protected implicit def long2String(x:Long):String = { String.valueOf(x) }
  protected val ds =  DatastoreServiceFactory.getDatastoreService

  def get(m:Map[String, scala.collection.immutable.List[String]]):ShpapadView = {
    this
  }

  def post(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    "trait post"
  }

  def delete(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    "trait delete"
  }
}

trait Auth extends ShpapadView {
  def isLoggedIn():Option[User] = {
    val us = UserServiceFactory.getUserService
    us.getCurrentUser match {
      case null => redirect = us.createLoginURL("/list"); None
      case u:User => Some(u)
    }
  }
}
