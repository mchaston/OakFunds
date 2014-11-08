var systemControllers = angular.module('systemControllers', ['ngRoute']);

systemControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/system_properties', {
        templateUrl: '/system/properties.ng',
        controller: 'SystemPropertiesCtrl'
      });
  }]);

systemControllers.controller('SystemPropertiesCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load system properties
  }]);
