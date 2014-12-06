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
    $scope.model = null;
    $scope.createFormVisible = false;
    $scope.updateFormVisible = false;

    $scope.showCreateForm = function() {
      $scope.updateFormVisible = false;
      $scope.model = {
        'title': 'New model',
      };
      $scope.createFormVisible = true;
    }

    $scope.hideCreateForm = function() {
      $scope.model = null;
      $scope.createFormVisible = false;
    }

    $scope.showUpdateForm = function(model) {
      $scope.createFormVisible = false;
      $scope.model = {
        'id': model.id,
        'title': model.attributes.title,
      };
      $scope.updateFormVisible = true;
    }

    $scope.hideUpdateForm = function() {
      $scope.model = null;
      $scope.updateFormVisible = false;
    }

    $scope.create = function() {
      var createRequest = {
        'title': $scope.model.title,
      };
      $http.post('/model/model/create', createRequest)
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
        'title': $scope.model.title,
      };
      $http.post('/model/model/' + $scope.model.id + '/update', updateRequest)
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
      $http.get('/model/models')
          .success(function(data) {
            $scope.models = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable();
  }]);

modelControllers.controller('ModelAccountsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    // TODO: load model accounts
  }]);
