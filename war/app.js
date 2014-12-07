var oakFundsApp = angular.module('oakFundsApp',
    [
    'ngRoute',
    'ngCookies',
    'accountControllers',
    'ledgerControllers',
    'modelControllers',
    'securityControllers',
    'securityDirectives',
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

function formatDate(dateString) {
  var date = new Date(dateString);
  return date.ddmmmyyyy();
}

var monthNames = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ];

Date.prototype.ddmmmyyyy = function() {
  var dd  = this.getDate().toString();
  return (dd[1]?dd:"0"+dd[0]) +
      ' ' + monthNames[this.getMonth()] +
      ' ' + this.getFullYear();
};
