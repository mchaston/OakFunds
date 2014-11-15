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
    $scope.refreshTable = function() {
      // refresh the system properties table
      $http.get('/system/system_properties')
          .success(function(data) {
            $scope.system_properties = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable();
  }]);
