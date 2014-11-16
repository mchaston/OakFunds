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
    // load account codes
    var account_codes_future = $http.get('/account/account_codes')
        .success(function(data) {
          $scope.account_codes = {};
          for (i = 0; i < data.length; i++) {
            var account_code = data[i];
            $scope.account_codes[account_code.id] = account_code.attributes.title;
          }
          // Load the main table only after loading the account codes.
          $scope.refreshTable();
        })
        .error(function(data, status, headers) {
          handleRequestErrors($window, data, status, headers);
        });

    $scope.refreshTable = function() {
      // refresh the accounts table
      $http.get('/ledger/accounts')
          .success(function(data) {
            $scope.accounts = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.accountCodeTitle = function(accountCodeId) {
      return $scope.account_codes[accountCodeId];
    }

    $scope.accountType = function(type) {
      switch (type) {
        case 'bank_account':
          return 'Bank';
        case 'expense_account':
          return 'Expense';
        case 'revenue_account':
          return 'Revenue';
        default:
          return 'Other';
      }
    }
  }]);
