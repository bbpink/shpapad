package sevensenses.model

import java.util.Date
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.{FilterOperator, SortDirection }
import com.google.appengine.api.users.{User, UserServiceFactory}

class Logout() extends ShpapadView {

  val us = UserServiceFactory.getUserService
  redirect = us.createLogoutURL("/")

}
