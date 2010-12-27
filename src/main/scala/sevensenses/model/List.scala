package sevensenses.model

import java.util.ConcurrentModificationException
import java.util.Date
import scala.collection.JavaConverters._
import sjson.json.JsonSerialization
import sjson.json.DefaultProtocol._
import org.fusesource.scalate._
import org.fusesource.scalate.servlet._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.{FilterOperator, SortDirection }
import com.google.appengine.api.users.{User, UserServiceFactory}


//リスト画面
class List() extends ShpapadView with Auth {

  //タスクリストのリスト
  var lists = scala.collection.immutable.List.empty[scala.collection.immutable.Map[String, String]]

  //Userオブジェクト取得(取得できない場合はログイン画面に遷移)
  private val user:User = isLoggedIn match {
    case Some(e) => e
    case None => null
  }

  //get
  override def get(m:Map[String, scala.collection.immutable.List[String]]):ShpapadView = {

    //メニュー設定
    this.username = user.getNickname
    this.menu = Map("/list" -> "リスト")

    //ログインユーザのタスクリストを取得してリスト化
    val q = new Query("list").addFilter("user", FilterOperator.EQUAL, user)
    ds.prepare(q).asList(FetchOptions.Builder.withLimit(1000)).asScala.foreach { x:Entity =>
      var lv = scala.collection.mutable.Map.empty[String, String]
      lv("key") = KeyFactory.keyToString(x.getKey)
      lv("listname") = x.getProperty("listname").asInstanceOf[String]
      lv("count") = x.getProperty("count").asInstanceOf[Long]
      lv("order") = x.getProperty("order").asInstanceOf[Long]
      lv("isDeleted") = x.getProperty("isDeleted").asInstanceOf[Long]
      lists = lists ::: scala.collection.immutable.List(lv.toMap)
    }

    this
  }

  //post
  override def post(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    var status = "error"
    var body = "わけわからんけどエラー"

    //入力チェック
    //ユーザ認証されている----------------------------------
    //パラメタにidがある------------------------------------
    //パラメタにvalueがある---------------------------------
    //valueが空文字でない-----------------------------------
    //idからlist_を除いた文字列でKeyを構築できる------------
    //構築したキーでds.getし、エンティティを取得できる------
    //ds.getしたエンティティが自分のものである--------------

    if (user != null) {
      if (m.contains("id") && m.contains("value")) {
        if (m("value").head != "") {

          try {

            val e:Entity = m("id").head match {
              case "insert" => new Entity("list")
              case _ => ds.get(KeyFactory.stringToKey(m("id").head.replace("list_", "")))
            }

            //挿入
            if (m("id").head == "insert") {

              e.setProperty("user", user)
              e.setProperty("listname", m("value").head)
              e.setProperty("count", 0)
              e.setProperty("order", 1)
              e.setProperty("isDeleted", 0)
              e.setProperty("modifiedDate", new Date)

            //更新
            } else {
              e.setProperty("listname", m("value").head)
              e.setProperty("modifiedDate", new Date)
            }

            if (e.getProperty("user").asInstanceOf[User].equals(this.user)) {
              body = context.capture(context.render("ListPiece.ssp"
                                                  , Map("m"->Map("key"->KeyFactory.keyToString(ds.put(e))
                                                               , "count"->e.getProperty("count").asInstanceOf[Long]
                                                               , "listname"->m("value").head))))

              //json化の準備(改行があるとダメ)
              body = body.replace("\r\n", "\n").replace("\n", "")
              status = "success"

            } else {
              body = "他の人のリストだよ！勘弁してください！"
            }

          } catch {
            case e:EntityNotFoundException => body = "リストが見つからない！"
            case e:IllegalArgumentException => body = "リストが見つからない！”"
            case e:DatastoreFailureException => body = "ごめん！もういっかい！"
            case e:ConcurrentModificationException => body = "他で更新してるみたい！"
          }

        } else {
          body = "リスト名を入力してね"
        }

      } else {
        body = "パラメタがおかしいよ"
      }
    } else {
      body = "ログインしていません"
    }

    //json化して返す
    JsonSerialization.tojson(Map("status"->status, "body"->body)).toString
  }

  //delete
  override def delete(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    var status = "error"
    var body = "わけわからんけどエラー"

    //入力チェック
    //ユーザ認証されている
    //入力パラメタにidがある
    //idからlist_を除いた文字列からKeyが生成できる
    //生成したKeyからds.getしてエンティティを取得できる
    //ds.getしたエンティティがログインユーザのものである
    //生成したKeyを親に持つTaskエンティティの数が0件

    if (user != null) {
      if (m.contains("id")) {

        try {
          val targetKey = KeyFactory.stringToKey(m("id").head.replace("list_", ""))
          val e = ds.get(targetKey)

          if (e.getProperty("user").asInstanceOf[User].equals(this.user)) {

            if (ds.prepare(new Query("task", e.getKey)).countEntities(FetchOptions.Builder.withLimit(1000)) == 0) {

              ds.delete(e.getKey)
              status = "success"
              body = "おｋ"

            } else {
              body = "タスクを全部削除してからリストを削除してね！"
            }

          } else {
            body = "他の人のリストだよ！勘弁してください！"
          }

        } catch {
          case e:EntityNotFoundException => body = "リストが見つからない！"
          case e:IllegalArgumentException => body = "変なパラメタだね！"
          case e:DatastoreFailureException => body = "ごめん！もういっかい！"
          case e:ConcurrentModificationException => body = "他で更新してるみたい！"
        }

      } else {
        body = "パラメタがおかしいよ"
      }
    } else {
      body = "ログインしていません"
    }

    JsonSerialization.tojson(Map("status"->status, "body"->body)).toString
  }

}
