package sevensenses.model

import java.util.Date
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.{FilterOperator, SortDirection }
import com.google.appengine.api.users.{User, UserServiceFactory}

class Top() extends ShpapadView with Auth {

  logoutable = false

  redirect = isLoggedIn match {
    case Some(e) => "/list"
    case None => ""
  }

}
