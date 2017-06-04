indexApp.controller("documentController", function ($scope, $rootScope, $http) {

    $scope.findAll = function () {
        $http.get("/document/findAll")
            .success(function (response) {
                $scope.data = response;
            })

    };

    $scope.findOne = function (id) {
        $http.get("/document/" + id)
            .success(function (response) {
                $scope.data = response;
            })

    };

    $scope.updateDoc = function (id) {
        $http.put("/document/" + id)
            .success(function (response) {
                layer.msg("Success!");
                $scope.findAll();
            })
            .error(function (response) {
                layer.msg(response.message);
            })
    };

    $scope.addDoc = function () {
        $http.post("/document/", $scope.doc)
            .success(function (response) {
                $scope.findAll();
            })
            .error(function (response) {
                layer.msg(response.message);
            })

    };


    $scope.delete = function (id) {
        $http.delete("/document/" + id)
            .success(function (response) {
                $scope.findAll();
            })
    };

    $scope.generatorSwaggerClientPop = function (id) {
        $("#clientLanguageId").val(id);
        $('#generatorSwaggerClientPop').modal('toggle')
    };

    $scope.generatorSwaggerServerPop = function (id) {
        $("#serverLanguageId").val(id);
        $('#generatorSwaggerServerPop').modal('toggle')
    };

    $scope.uploadSwaggerJson = function () {
        $('#uploadSwaggerJsonPop').modal('toggle')
    };


    $scope.doc = {"title": "Test", "url": "http://localhost:8080/v2/api-docs"};

    $scope.generatorDocPop = function (id) {
        $("#docId").val(id);
        $('#generatorDocPop').modal('toggle')
    };

    $scope.findAll();
});