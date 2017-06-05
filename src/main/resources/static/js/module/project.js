indexApp.controller("projectDBController", function ($scope, $rootScope, $http) {


    $scope.addDBPop = function () {
        $('#editDBModal').modal('toggle')
    }

    $scope.addDB = function () {
        $http.post("/db/", $scope.db)
            .success(function (response) {
                $scope.listDb();
            })
    }


    $scope.listDb = function () {
        $http.get("/db/findAll")
            .success(function (response) {
                $scope.data = response;

            })

    };

    $scope.findOne = function (id) {
        $http.get("/db/" + id)
            .success(function (response) {
                $scope.data = response;
            })

    };


    $scope.delDb = function (id) {
        $http.delete("/db/" + id)
            .success(function (response) {
                $scope.listDb();

            })
    };

    $scope.generatorModalPop = function (id) {
        $("#genSpringBootId").val(id);
        $scope.select2(id);
        $('#generatorSpringBootPop').modal('toggle')
    };


    $scope.select2 = function (id) {
        $(".select2").select2({
            ajax: {
                method: "get",
                url: "/db/" + id + "/tables",
                delay: 250,
                data: function (params) {
                    return {
                        q: params.term, // search term
                        page: params.page
                    };
                },
                processResults: function (data, params) {
                    var result = [];
                    $.each(data, function (index, value) {
                        // alert(value+" "+value['name']+" "+value['value']);
                        result.push({id: value, text: value});
                    });
                    return {
                        results: result
                    };
                }
            }
        });


    };

    $scope.generatorWebModalPop = function (id) {
        $("#genWebId").val(id);
        $scope.select2(id);
        $('#generatorWebModalPop').modal('toggle')
    };

    $scope.generatorUIModalPop = function (id) {
        $("#genUIId").val(id);
        $scope.select2(id);
        $('#generatorUIModalPop').modal('toggle')
    };

    $scope.generatorSpringCloudPop = function (id) {
        $("#genSpringCloudId").val(id);
        $scope.select2(id);
        $('#generatorSpringCloudPop').modal('toggle')
    };

    $scope.generatorDubboPop = function (id) {
        $("#genDubboId").val(id);
        $scope.select2(id);
        $('#generatorDubboPop').modal('toggle')
    };

    $scope.generatorSwaggerClientPop = function () {
        $('#generatorSwaggerClientPop').modal('toggle')
    };


    $scope.listDb();


});