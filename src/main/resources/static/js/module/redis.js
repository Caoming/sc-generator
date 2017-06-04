indexApp.controller("redisDataSourceController", function ($scope, $rootScope, $http) {


    $scope.addDBPop = function () {
        $('#editDBModal').modal('toggle')
    }

    $scope.addDB = function () {
        $http.post("/redisDataSource/", $scope.db)
            .success(function (response) {
                $scope.listDb();
            })
    }


    $scope.listDb = function () {
        $http.get("/redisDataSource/findAll")
            .success(function (response) {
                $scope.data = response;

            })

    };

    $scope.findOne = function (id) {
        $http.get("/redisDataSource/" + id)
            .success(function (response) {
                $scope.data = response;
            })

    };


    $scope.delDb = function (id) {
        $http.delete("/redisDataSource/" + id)
            .success(function (response) {
                $scope.listDb();

            })
    };

    var total = 0;
    var currentId;
    var ws;
    $scope.redisPop = function (id) {
        $("#redisPop").modal('toggle');
        if (ws) {
            if (currentId !== id) {
                ws.close();
                ws = new WebSocket("ws://" + window.location.host + "/ws/redis?id=" + id);
            }
        } else {
            ws = new WebSocket("ws://" + window.location.host + "/ws/redis?id=" + id)
        }

        currentId = id;
        ws.onopen = function (p1) {
            $("#total").html(0);
        };

        ws.onmessage = function (event) {
            if ($scope.lastCommand === 'sync') {
                var html = $("#syncDiv").html();
                if (event.data === '\r\n' || event.data === "\n") {
                    $("#syncDiv").html(html + "<br>");
                    return ;
                } else {
                    $("#syncDiv").html(html + event.data);
                    total += 1;
                    $("#total").html(total);
                }
                $("#syncTime").html(new Date().toUTCString());
                return
            }

            if (event.data !== '\n' && event.data !== '\r\n' && event.data !== '') {
                messageLeft(event.data.split("\n").join("<br>"));
                $scope.top();
                total += 1;
                $("#total").html(total);
            }



        };

        ws.onclose = function (p1) {
            ws.close()
        };

        ws.onerror = function (p1) {
            ws.close()
        }
    };

    $scope.currentDB = "select 0\r\n";
    $scope.lastCommand = null;
    $scope.sendCommand = function (command) {
        $scope.lastCommand = command;
        $scope.command = "";

        if (command === 'clear') {
            $("#message").html("");
            return;
        } else if (command === 'sync') {
            $("#message").html("");
            syncLeft();
            ws.send(command);
            return;
        } else if (command.indexOf('select') !== -1) {
            $scope.currentDB = command + "\r\n";
            messageLeft("OK");
            $scope.top();
            return;
        }

        messageRight(command)
        ws.send(($scope.currentDB==='select 0\r\n'?"":$scope.currentDB) + command);
    };

    var messageRight = function (data) {
        $("#message").append('<div class="direct-chat-msg right"> <div class="direct-chat-info clearfix"> <span class="direct-chat-name pull-right">Admin</span> <span class="direct-chat-timestamp pull-left">' + new Date().toUTCString() + '</span> </div> <img class="direct-chat-img" src="/webjars/adminlte/2.3.6/dist/img/user2-160x160.jpg"alt="Message User Image"> <div class="direct-chat-text"> ' + data + '</div> </div>')
    };

    var messageLeft = function (data) {
        $("#message").append('<div class="direct-chat-msg left"> <div class="direct-chat-info clearfix"> <span class="direct-chat-name pull-left">Redis</span> <span class="direct-chat-timestamp pull-right">' + new Date().toUTCString() + '</span> </div> <img class="direct-chat-img" src="/img/redis.png" alt="Message User Image"> <div class="direct-chat-text">  ' + data + '</div> </div>')
    };

    var syncLeft = function () {
        $("#message").append('<div class="direct-chat-msg left"> <div class="direct-chat-info clearfix"> <span class="direct-chat-name pull-left">Redis</span> <span class="direct-chat-timestamp pull-right" id="syncTime">' + new Date().toUTCString() + '</span> </div> <img class="direct-chat-img" src="/img/redis.png" alt="Message User Image"> <div class="direct-chat-text" id="syncDiv"> </div> </div>')
    };

    $scope.clearPing = function () {
        var html = $("#syncDiv").html().trim().split("*1\n<br>$4\n<br>PING\n<br>").join("");
        $("#syncDiv").html(html);
    };

    $scope.top = function () {
        $('#message').scrollTop($('#message')[0].scrollHeight);
    };
    $scope.listDb();
});