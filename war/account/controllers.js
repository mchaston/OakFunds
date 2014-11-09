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
    $scope.account_code = {
      'title': 'New account code',
    };

    $scope.create = function() {
      var createRequest = {
        'number': $scope.account_code.number,
        'title': $scope.account_code.title,
      };
      $http.post('/account/create_account_code', createRequest)
          .success(function(data) {
            // refresh the account codes table
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable = function() {
      // refresh the account codes table
      $http.get('/account/account_codes')
          .success(function(data) {
            $scope.account_codes = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable();
  }]);

function handleRequestErrors($window, data, status, headers) {
  if (status == 401) {
    // not logged in
    if (headers('next_url') != null) {
      $window.location.href = headers('next_url');
    } else {
      alert('unauthenticated: please log in');
    }
  } else if (status == 403) {
    // not logged in as admin
    alert('unauthorized: please log in as an admin');
    if (headers('next_url') != null) {
      $window.location.href = headers('next_url');
    }
  } else {
    // some other error occurred
    alert('error occurred: ' + status);
  }
}
