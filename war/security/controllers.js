var securityControllers = angular.module('securityControllers', ['ngRoute']);

securityControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/users', {
        templateUrl: '/security/users.ng',
        controller: 'SecurityUsersCtrl'
      });
  }]);

securityControllers.controller('SecurityUsersCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load users
  }]);
