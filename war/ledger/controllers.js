var ledgerControllers = angular.module('ledgerControllers', ['ngRoute']);

ledgerControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/accounts', {
        templateUrl: '/ledger/accounts.ng',
        controller: 'LedgerAccountsCtrl'
      });
  }]);

ledgerControllers.controller('LedgerAccountsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load accounts
  }]);
