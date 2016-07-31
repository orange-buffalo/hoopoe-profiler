function AppConfig($routeProvider, $locationProvider, $mdThemingProvider, $mdIconProvider) {

  //https://angular-md-color.com/#/

  var hoopoePrimaryPalette = {
    '50': '#c79f9b',
    '100': '#be8f8b',
    '200': '#b5807a',
    '300': '#ac706a',
    '400': '#a2615a',
    '500': '#925751',
    '600': '#824d48',
    '700': '#71433f',
    '800': '#613a36',
    '900': '#50302d',
    'A100': '#d1afab',
    'A200': '#dabfbc',
    'A400': '#e3cecc',
    'A700': '#402623'
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

function AppRunner($rootScope, helperService) {
  $rootScope.helperService = helperService;

  $rootScope.hoopoeConfig = {
    expandDirectPaths : true
  }
}