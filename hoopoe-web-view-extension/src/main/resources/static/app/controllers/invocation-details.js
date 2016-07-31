function InvocationDetailsController($scope, $http, $routeParams, $mdDialog, helperService, $location) {
  var $controller = this;

  function _expandNode(node) {
    if ($controller.expandedInvocations.indexOf(node) == -1) {
      $controller.expandedInvocations.push(node);
    }
  }

  function _expandDirectPaths(node) {
    if ($scope.hoopoeConfig.expandDirectPaths) {
      _expandNode(node);
      if (node.children && node.children.length == 1) {
        _expandDirectPaths(node.children[0]);
      }
    }
  }

  $controller.invocationsTreeOptions = {
    templateUrl: 'invocations-tree-template.html'  //todo prefetch it and move to separate file
  };

  $controller.expandedInvocations = [];

  $controller.onInvocationNodeToggle = function (node, expanded) {
    if (expanded) {
      _expandDirectPaths(node);
    }
    $controller.selectedInvocation = null;
  };

  $controller.selectMethod = function (invocation, selected) {
    if (!selected) {
      return;
    }
    $mdDialog.show({
      templateUrl: 'views/method-details.html',
      clickOutsideToClose: true,
      controller: function ($scope, $mdDialog) {
        $scope.invocation = invocation;
        $scope.helperService = helperService;

        $scope.closeDialog = function () {
          $mdDialog.hide();
        }

      }
    }).then(null, function () {
      $controller.selectedInvocation = null;
    })
  };

  $controller.backToList = function () {
    $location.path("/invocations");
  };

  $http.get('api/invocations/' + $routeParams.invocationId)
    .then(
      function (invocation) {
        $controller.data = invocation.data;
        _expandDirectPaths($controller.data);
      },
      function () {
        //todo handle errors
      }
    );

}