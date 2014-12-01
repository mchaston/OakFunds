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
    $scope.user = null;
    $scope.updateFormVisible = false;

    // load the roles
    $http.get('/security/role_names')
        .success(function(data) {
          $scope.role_names = data;
        })
        .error(function(data, status, headers) {
          handleRequestErrors($window, data, status, headers);
        });

    $scope.showUpdateForm = function(user) {
      $scope.user = {
        'id': user.id,
        'email': user.attributes.email,
        'name': user.attributes.name,
        'role_names': user.role_names,
      };
      $scope.updateFormVisible = true;
    }

    $scope.hideUpdateForm = function() {
      $scope.user = null;
      $scope.updateFormVisible = false;
    }

    $scope.update = function() {
      var updateRequest = {
        'email': $scope.user.email,
        'name': $scope.user.name,
        'role_names': $scope.user.role_names,
      };
      $http.post('/security/user/' + $scope.user.id + '/update', updateRequest)
          .success(function(data) {
            $scope.hideUpdateForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable = function() {
      // refresh the users table
      $http.get('/security/users')
          .success(function(data) {
            $scope.users = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable();
  }]);
