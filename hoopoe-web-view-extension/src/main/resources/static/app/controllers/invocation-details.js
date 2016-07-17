function InvocationDetailsController($http, $routeParams) {
  var $scope = this;

  $http.get('api/invocations/' + $routeParams.invocationId)
    .then(
      function (invocation) {
        $scope.data = invocation.data;
        // todo remove
        console.log($scope.data);
      },
      function () {
        //todo handle errors
      }
    );

}