function InvocationsListController($http, $location) {
  var $scope = this;
  $scope.data = [];

  $scope.showDetails = function (invocationId) {
    $location.path("/invocations/" + invocationId);
  };

  $http.get('api/invocations')
    .then(
      function (invocationsList) {
        $scope.data = invocationsList.data;
        // todo remove
        console.log($scope.data);
      },
      function () {
        //todo handle errors
      }
    );

}