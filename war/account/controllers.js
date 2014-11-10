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
    $scope.account_code = null;
    $scope.createFormVisible = false;
    $scope.updateFormVisible = false;

    $scope.showCreateForm = function() {
      $scope.updateFormVisible = false;
      $scope.account_code = {
        'title': 'New account code',
      };
      $scope.createFormVisible = true;
    }

    $scope.hideCreateForm = function() {
      $scope.account_code = null;
      $scope.createFormVisible = false;
    }

    $scope.showUpdateForm = function(account_code) {
      $scope.createFormVisible = false;
      $scope.account_code = {
        'number': account_code.id,
        'title': account_code.attributes.title,
      };
      $scope.updateFormVisible = true;
    }

    $scope.hideUpdateForm = function() {
      $scope.account_code = null;
      $scope.updateFormVisible = false;
    }

    $scope.create = function() {
      var createRequest = {
        'number': $scope.account_code.number,
        'title': $scope.account_code.title,
      };
      $http.post('/account/account_code/create', createRequest)
          .success(function(data) {
            $scope.hideCreateForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.update = function() {
      var updateRequest = {
        'title': $scope.account_code.title,
      };
      $http.post('/account/account_code/' + $scope.account_code.number + '/update', updateRequest)
          .success(function(data) {
            $scope.hideUpdateForm();
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
