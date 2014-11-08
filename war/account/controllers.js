var accountControllers = angular.module('accountControllers', ['ngRoute']);

accountControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/account_codes', {
        templateUrl: '/account/account_codes.ng',
        controller: 'AccountCodesCtrl'
      });
  }]);

accountControllers.controller('AccountCodesCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load account codes
  }]);
