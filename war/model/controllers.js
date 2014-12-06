var modelControllers = angular.module('modelControllers', ['ngRoute']);

modelControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/models', {
        templateUrl: '/model/models.ng',
        controller: 'ModelsCtrl'
      }).
      when('/model_accounts', {
        templateUrl: '/model/accounts.ng',
        controller: 'ModelAccountsCtrl'
      });
  }]);

modelControllers.controller('ModelsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load models
  }]);

modelControllers.controller('ModelAccountsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load model accounts
  }]);
