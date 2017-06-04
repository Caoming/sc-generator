// $locationProvider.html5Mode(true);
//菜单
var indexApp = angular.module("indexApp", ['ngRoute'])

indexApp.config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {

    $routeProvider
        .when('/project.html', {
            controller: "projectDBController",
            templateUrl: '/module/project.html'
        })
        .when('/document.html', {
            controller: "documentController",
            templateUrl: '/module/document.html'
        })
        .when('/redis.html', {
            controller: "redisDataSourceController",
            templateUrl: '/module/redis.html'
        })
        .otherwise('/project.html', {
            controller: "projectDBController",
            templateUrl: '/module/project.html'
        })

}]);