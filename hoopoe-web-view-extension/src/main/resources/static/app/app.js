angular
  .module('HoopoeApp', ['ngMaterial', 'ngRoute', 'treeControl'])
  .config(['$routeProvider', '$locationProvider', '$mdThemingProvider', '$mdIconProvider', AppConfig])
  .factory('HelperService', HelperService)
  .controller('InvocationsListCtrl', ['$http', '$location', InvocationsListController])
  .controller('InvocationDetailsCtrl', ['$scope', '$http', '$routeParams', InvocationDetailsController])
  .run(['$rootScope', 'HelperService', AppRunner]);