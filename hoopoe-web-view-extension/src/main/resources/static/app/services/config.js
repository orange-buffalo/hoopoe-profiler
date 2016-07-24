function AppConfig($routeProvider, $locationProvider, $mdThemingProvider, $mdIconProvider) {

  //https://angular-md-color.com/#/

  var hoopoePrimaryPalette = {
    '50': '#30303f',
    '100': '#3b3b4e',
    '200': '#46465c',
    '300': '#51526b',
    '400': '#5c5d79',
    '500': '#676888',
    '600': '#8182a0',
    '700': '#9091ab',
    '800': '#9f9fb6',
    '900': '#adaec1',
    'A100': '#8182a0',
    'A200': '#737495',
    'A400': '#676888',
    'A700': '#bcbccc'
  };
  $mdThemingProvider.definePalette('hoopoePrimaryPalette', hoopoePrimaryPalette);

  $mdThemingProvider
    .theme('default')
    .primaryPalette('hoopoePrimaryPalette')
    .dark();

  $routeProvider
    .when('/invocations/:invocationId', {
      templateUrl: '/views/invocation-details.html'
    })
    .otherwise({
      templateUrl: '/views/invocations-list.html'
    });

  $locationProvider.html5Mode(true);

  $mdIconProvider.defaultIconSet('/img/hoopoe-icons.svg');
}