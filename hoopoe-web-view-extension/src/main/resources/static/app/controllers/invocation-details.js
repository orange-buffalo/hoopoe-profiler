function InvocationDetailsController($http, $routeParams) {
  var $scope = this;

  $scope.invocationsTreeOptions = {
    templateUrl: 'invocations-tree-template.html'  //todo prefetch it and move to separate file
  };

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