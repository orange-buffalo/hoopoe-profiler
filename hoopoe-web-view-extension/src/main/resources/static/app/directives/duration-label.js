function DurationLabel() {
  return {
    restrict: 'E',
    replace: true,
    scope: {
      rawDurationInNs: "=durationInNs"  //todo better get it from content instead of attribute
    },
    templateUrl: "views/components/duration-label.html",
    controller: function ($scope, $rootScope) {
      $scope.smartDuration = $rootScope.helperService.getSmartDuration($scope.rawDurationInNs);
      $scope.durationInSec = $rootScope.helperService.printFloat($scope.rawDurationInNs / 1000000 / 1000) + ' s';
      $scope.durationInMs = $rootScope.helperService.printFloat($scope.rawDurationInNs / 1000000) + ' ms';
      $scope.durationInNs = $rootScope.helperService.printFloat($scope.rawDurationInNs) + ' ns';
    }
  };
}