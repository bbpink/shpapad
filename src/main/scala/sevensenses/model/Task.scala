package sevensenses.model

import java.util.Date
import scala.collection.JavaConverters._
import sjson.json.JsonSerialization
import sjson.json.DefaultProtocol._
import org.fusesource.scalate._
import org.fusesource.scalate.servlet._
import com.google.appengine.api.datastore._
import com.google.appengine.api.datastore.Query.{FilterOperator, SortDirection }
import com.google.appengine.api.users.{User, UserServiceFactory}

class Task() extends ShpapadView with Auth {

  private val user = isLoggedIn match {
    case Some(e) => e;
    case None => null;
  }

  var tasks = scala.collection.immutable.List.empty[scala.collection.immutable.Map[String, String]]
  var isListExists = false
  var errorMessage = "なんかわからんけどエラー"
  var listName = ""

  //get
  override def get(m:Map[String, scala.collection.immutable.List[String]]):ShpapadView = {

    try {
      if (m.contains("l")) {
        val le = ds.get(KeyFactory.stringToKey(m("l").head))
        listName = le.getProperty("listname").asInstanceOf[String]

        //task取得
        ds.prepare(new Query("task", le.getKey)).asList(FetchOptions.Builder.withLimit(1000)).asScala.foreach { x:Entity =>
          var lv = scala.collection.mutable.Map.empty[String, String]
          lv("key") = KeyFactory.keyToString(x.getKey)
          lv("taskvalue") = x.getProperty("taskvalue").asInstanceOf[String]
          lv("isFinished") = (if (x.getProperty("isFinished").asInstanceOf[Long] == 1) "true" else "false")
          lv("isDeleted") = x.getProperty("isDeleted").asInstanceOf[Long]
          lv("order") = x.getProperty("order").asInstanceOf[Long]
          tasks = tasks ::: scala.collection.immutable.List(lv.toMap)
        }

        //メニュー作り
        username = user.getNickname
        menu = Map("/list" -> "リスト", "/task?l=" + m("l").head -> listName)

        this.isListExists = true

      } else {
        this.errorMessage = "リストを指定してください！！！"
      }

    //キー指定なしエラー
    } catch {
      case e:IllegalArgumentException => this.errorMessage = "パラメタがおかしいね！"
      case e:EntityNotFoundException => this.errorMessage = "そんなリストはないみたい！"
      case e:DatastoreFailureException => this.errorMessage = "ごめん！もういっかい！"
    }

    this
  }

  //post
  override def post(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    var status = "error"
    var body = "わけわからんけどエラー"

    //入力チェック
    //ユーザ認証されている
    //パラメタにlがある
    //パラメタにidがある
    //パラメタにvalueがある
    //valueが空文字でない
    //lからキーが構築できる
    //lから構築したキーでListのエンティティが取得できる
    //取得したListのエンティティが自分のものである
    //idからtask_を除いた文字列でKeyを構築できる
    //構築したKeyでds.getしてTaskのエンティティを取得できる

    if (user != null) {
      if (m.contains("l") && m.contains("id") && m.contains("value")) {
        if (m("value").head != "") {

          try {

            //トランザクション開始
            val tx = ds.beginTransaction

            //Listのエンティティを取得
            val le = ds.get(KeyFactory.stringToKey(m("l").head))

            if (le.getProperty("user").asInstanceOf[User].equals(this.user)) {

              val te:Entity = m("id").head match {
                case "insert" => new Entity("task", KeyFactory.stringToKey(m("l").head))
                case _ => ds.get(KeyFactory.stringToKey(m("id").head.replace("task_", "")))
              }

              //タスク追加
              if (m("id").head == "insert") {
                te.setProperty("user", user)
                te.setProperty("taskvalue", m("value").head)
                te.setProperty("isFinished", 0)
                te.setProperty("isDeleted", 0)
                te.setProperty("order", 1)

                //タスクリストのカウントをインクリメント
                le.setProperty("count", le.getProperty("count").asInstanceOf[Long] + 1)
                ds.put(le)

              } else {
                te.setProperty("taskvalue", m("value").head)
                if (m.contains("isFinished")) {
                  te.setProperty("isFinished", if (m("isFinished").head == "true") 1 else 0)
                }
              }

              te.setProperty("modifiedDate", new Date)

              body = context.capture(context.render("TaskPiece.ssp"
                                                  , Map("m"->Map("key"->KeyFactory.keyToString(ds.put(te))
                                                               , "isFinished"->(if (te.getProperty("isFinished").asInstanceOf[Int] == 1) "true" else "false")
                                                               , "taskvalue"->m("value").head
                                                                 )
                                                        )
                                                    )
                                     )

              //コミット
              tx.commit

              //JSON化の準備
              body = body.replace("\r\n", "\n").replace("\n", "")

              status = "success"

            } else {
              body = "他の人のタスクだよ！勘弁してください！"
            }

          } catch {
            case e:IllegalArgumentException => body = "パラメタがおかしいね！"
            case e:EntityNotFoundException => body = "タスクがみつからない！"
            case e:DatastoreFailureException => body = "ごめん！もういっかい！"
          }

        } else {
           body = "タスク名を入力してね！"
        }
      } else {
         body = "パラメタがおかしいよ！"
      }
    } else {
       body = "ログインしてね！"
    }

    JsonSerialization.tojson(Map("status"->status, "body"->body)).toString
  }

  //delete
  override def delete(m:Map[String, scala.collection.immutable.List[String]], context:ServletRenderContext):String = {
    var status = "error"
    var body = "わけわからんけどエラー"

    //入力チェック
    //ユーザ認証されている
    //入力パラメタにidがある
    //idからtask_を除いた文字列からKeyが生成できる
    //生成したKeyからds.getしてエンティティを取得できる
    //取得したエンティティが自分のものである

    if (user != null) {
      if (m.contains("id")) {

        try {
          val te = ds.get(KeyFactory.stringToKey(m("id").head.replace("task_", "")))
          if (te.getProperty("user").asInstanceOf[User].equals(this.user)) {

            val le = ds.get(te.getParent)

            //トランザクション開始
            val tx = ds.beginTransaction

            ds.delete(te.getKey)
            le.setProperty("count", le.getProperty("count").asInstanceOf[Long] - 1)
            ds.put(le)

            //コミット
            tx.commit

            status = "success"
            body = "おｋ"

          } else {
            body = "他の人のタスクだよ！勘弁してください！"
          }
        } catch {
          case ex:IllegalArgumentException => body = "パラメタがおかしいね！"
          case ex:EntityNotFoundException => body = "タスクがみつからない！"
          case ex:DatastoreFailureException => body = "ごめん！もういっかい！"
        }

      } else {
        body = "パラメタがおかしいよ！"
      }
    } else {
      body = "ログインしてね！"
    }

    JsonSerialization.tojson(Map("status"->status, "body"->body)).toString
  }

}
