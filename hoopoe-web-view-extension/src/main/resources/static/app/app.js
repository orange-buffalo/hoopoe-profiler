angular
  .module('HoopoeApp', [
    'ngMaterial', 'ngRoute', 'treeControl', 'angular-jsonrpc-client', 'ngSanitize'
  ])
  .config([
    '$routeProvider', '$locationProvider', '$mdThemingProvider', '$mdIconProvider', 'jsonrpcConfigProvider',
    AppConfig
  ])
  .factory('helperService', [
    HelperService
  ])
  .factory('errorHelper', [
    ErrorHelper
  ])
  .factory('operationsProgressService', [
    OperationsProgressService
  ])
  .factory('profilerRpc', [
    'jsonrpc', 'errorHelper', '$q',
    ProfilerRpc
  ])
  .controller('BaseCtrl', [
    'operationsProgressService',
    BaseCtrl
  ])
  .controller('ProfilerCtrl', [
    'profilerRpc', 'operationsProgressService', '$scope', '$mdDialog', 'helperService',
    ProfilerCtrl
  ])
  .run([
    '$rootScope', 'helperService',
    AppRunner
  ]);