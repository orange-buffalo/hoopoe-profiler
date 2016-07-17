angular
  .module('HoopoeApp', ['ngMaterial', 'ngRoute', 'treeControl'])
  .config(['$routeProvider', '$locationProvider', '$rootScopeProvider',
    function ($routeProvider, $locationProvider, $rootScopeProvider) {

    //todo calculate and set proper value
    //$rootScopeProvider.digestTtl(10000000);

      $routeProvider
        .when('/invocations/:invocationId', {
          templateUrl: '/views/invocation-details.html'
        })
        .otherwise({
          templateUrl: '/views/invocations-list.html'
        });

      $locationProvider.html5Mode(true);
    }])
  .factory('HelperService', HelperService)
  .controller('InvocationsListCtrl', ['$http', '$location', InvocationsListController])
  .controller('InvocationDetailsCtrl', ['$http', '$routeParams', InvocationDetailsController])
  .directive('hpDurationLabel', ['$rootScope', DurationLabel])
  .run(['$rootScope', 'HelperService', function ($rootScope, helperService) {
    $rootScope.helperService = helperService;
  }])