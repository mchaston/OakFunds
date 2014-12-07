angular.module('securityDirectives', [])
  .directive('ofPermission', ['$http', '$q', '$window', function($http, $q, $window) {
    var deferred = $q.defer();
    var promise = deferred.promise;

    // Load the permissions that the user has.
    $http.get('/security/user_permissions')
        .success(function(data) {
          deferred.resolve(data);
        })
        .error(function(data, status, headers) {
          handleRequestErrors($window, data, status, headers);
        });

    function link(scope, element, attrs) {
      promise.then(function (user_permissions) {
        // If the permissions do not contain the permission, then remove the element.
        if (user_permissions.indexOf(attrs.ofPermission) == -1) {
          element.remove();
        }
      })
    }

    return {
      link: link
    };
  }]);
