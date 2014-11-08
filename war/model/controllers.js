var modelControllers = angular.module('modelControllers', ['ngRoute']);

modelControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/model_accounts', {
        templateUrl: '/model/accounts.ng',
        controller: 'ModelAccountsCtrl'
      });
  }]);

modelControllers.controller('ModelAccountsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load model accounts
  }]);
