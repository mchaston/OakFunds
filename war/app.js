var oakFundsApp = angular.module('oakFundsApp',
    [
    'ngRoute',
    'ngCookies',
    'accountControllers',
    'ledgerControllers',
    'modelControllers',
    'securityControllers',
    'systemControllers'
    ]);

oakFundsApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/welcome', {
        templateUrl: '/welcome.html',
      }).
      otherwise({
        redirectTo: '/welcome'
      });
  }]);
