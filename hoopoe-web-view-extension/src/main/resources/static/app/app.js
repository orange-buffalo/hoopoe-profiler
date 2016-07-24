angular
  .module('HoopoeApp', ['ngMaterial', 'ngRoute', 'treeControl'])
  .config(['$routeProvider', '$locationProvider', '$mdThemingProvider', '$mdIconProvider', AppConfig])
  .factory('HelperService', HelperService)
  .controller('InvocationsListCtrl', ['$http', '$location', InvocationsListController])
  .controller('InvocationDetailsCtrl', ['$http', '$routeParams', InvocationDetailsController])
  .directive('hpDurationLabel', ['$rootScope', DurationLabel])
  .run(['$rootScope', 'HelperService', function ($rootScope, helperService) {
    $rootScope.helperService = helperService;
  }])