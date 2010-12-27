$(document).ready(function(){

    var destination = document.URL;

    //タスク移動
    $(".finish").each(function() {
        if ($(this).attr("checked")) {
            var pr = $(this).parent();
            pr.appendTo("#finished");
        }
    });

    //update
    $("a.startArrange").live("click", function(e) {
        var pr = $(e.target).parent();
        if (pr.attr("id") != "insert") {
            pr.children("span.arranging").children("input").val(pr.find("span.name").text());
        }
        pr.children(".showing").hide();
        pr.children(".arranging").show();
    });

    //update cancel
    $("a.cancel").live("click", function(e) {
        var pr = $(e.target).parent();
        pr.children("span.arranging").children("input").val("");
        pr.children(".showing").show();
        pr.children(".arranging").hide();
    });

    //save function
    var postData = function(e) {
        var pre = $(e.target).parent();
        var pr = null;
        if (pre[0].tagName == "SPAN") {
            pr = pre.parent();
        } else {
            pr = pre;
        }

        $.post(destination,
            {  id : pr.attr("id")
             , value : pr.children("span.arranging").children("input.inputValue").val()
             , isFinished : pr.find("input.finish").attr("checked")
            },

            //callback
            function(text) {

                //返ってきたjsonを評価
                var f = eval("(" + text + ")");

                if (f.status == "success") {

                    //入力したテキストボックスを空にする
                    pr.children("span.arranging").children("input.inputValue").val("");

                    if (pr.attr("id") == "insert") {

                        //insertの場合は直前にli要素を追加
                        $("#insert").before(f.body);
                        $("#insert").prev().children(".arranging").hide();
                        $("#insert").prev().children(".showing").hide();
                        $("#insert").prev().children(".showing").fadeIn();

                        //insertの場合は入力テキストボックスを開放したまま(続けて入力可)

                    } else {

                        //updateの場合は直後にli要素を追加し、自身をremove
                        pr.after(f.body);
                        var cur = pr.next();

                        //更新後、終了フラグが終了から残へ
                        if ((cur.find(".finish").attr("checked") == false) && (pr.parent().attr("id") == "finished")) {
                            $("#insert").before(cur);

                        //更新後、終了フラグが残から終了へ
                        } else if ((cur.find(".finish").attr("checked") == true) && (pr.parent().attr("id") == "remaining")) {
                            cur.appendTo("#finished");
                        }

                        cur.hide().fadeIn();
                        pr.remove();

                    }
                } else {
                    alert("エラー！(＞＜）@" + f.body);
                }
        });
    };

    //save
    $("a.save").live("click", function(e) {
        postData(e);
    });

    //press enter
    $("input.inputValue").live("keyup", function(e) {
        var keypressed = null;
        if (window.event) keycode = window.event.keyCode;
        else if (e) keycode = e.which;
        keypressed = keycode;

        if (keycode == 13) {
            postData(e);
        }
    });

    //finish
    $("input.finish").live("click", function(e) {
        postData(e);
    });

    //delete
    $("a.delete").live("click", function(e) {
        var pr = $(e.target).parent();
        var taskCount = pr.children("span.taskCount").text().replace("(", "").replace(")", "");
        if (taskCount != 0) {
            alert("全てのタスクを削除してから削除してください！");
        } else {
            if (confirm("本当に削除してもよろしいですか？")) {

                $.ajax({ type : "DELETE"
                      , url : destination + ((destination.indexOf("?") == -1) ? "?" : "&") + "id=" + pr.attr("id")
                      , success : function(json) {
                                      var f = eval("(" + json + ")");
                                      if (f.status == "success") {
                                          pr.fadeOut();
                                      } else {
                                          alert("エラー！(＞＜）@" + f.body);
                                      }
                                  }
                });
            }
        }
    });
});
