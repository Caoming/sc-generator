app.controller("${modelName}Controller", function ($scope, $rootScope, $http) {

    $scope.basePath = "/${varModelName}/";


    $scope.list = function (pageNum) {
        $http.get($scope.basePath.concat("list"), {params: {pageNum: pageNum}})
            .success(function (res) {
                $scope.page = res;
            })
    };

    $scope.selectByPrimaryKey = function (id) {
        $http.get($scope.basePath.concat(id))
            .success(function (res) {
                if (res) {
                    $scope.page.list = [res];
                } else {
                    $scope.page.list = [];
                }
                $scope.page.pages = 1;
            })
            .error(function (res) {
                $scope.page.list = [];
            })

    };

    $scope.updateByPrimaryKey = function (data) {
        $http.put($scope.basePath, data)
            .success(function (res) {
                $scope.list();
            })
            .error(function (res) {
            })
    };

    $scope.insert = function (data) {
        $http.post($scope.basePath, data)
            .success(function (response) {
                $scope.list();
            })
            .error(function (res) {
            })
    };

    $scope.insertOrUpdate = function (data) {
        if (data.$primaryKey) {
            $scope.updateByPrimaryKey(data);
        } else {
            $scope.insert(data);
        }
    };

    $scope.add = function () {
        $scope.data = {};
        $scope.pop();
    };


    $scope.deleteByPrimaryKey = function (id) {
        $http.delete($scope.basePath.concat(id))
            .success(function (res) {
                $scope.list();
                $scope.hidden()
            })
            .error(function (res) {
            })
    };

    $scope.edit = function (id) {
        $http.get($scope.basePath.concat(id))
            .success(function (res) {
                $scope.data = res;
                $scope.pop();
            })
            .error(function (res) {
            })
    };

    $scope.pop = function () {
        var pop = '${varModelName}Pop';
        $('#' + pop).modal('toggle');
    };

    $scope.confirm = function (id, title, msg) {
        $scope.param = id;
        $scope.msg = msg;
        $scope.title = title;
        $('#confirmModal').modal('toggle');
    };

    $scope.hidden = function () {
        $scope.param = null;
        $scope.msg = null;
        $scope.title = null;
        $('#confirmModal').modal('hide');
    };

    $scope.tip = function (msg) {
        $scope.title = "Message";
        $scope.msg = msg;
        $('#messageModal').modal('toggle');
    };

    $scope.list();
});