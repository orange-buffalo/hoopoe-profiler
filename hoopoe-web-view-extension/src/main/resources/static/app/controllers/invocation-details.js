function InvocationDetailsController($scope, $http, $routeParams, $mdDialog, helperService, $location) {
  var $controller = this;

  function _expandInvocation(invocation) {
    if ($controller.expandedInvocations.indexOf(invocation) == -1) {
      $controller.expandedInvocations.push(invocation);
      if (invocation.parent) {
        _expandInvocation(invocation.parent);
      }
    }
  }

  function _expandDirectPaths(invocation) {
    if ($scope.hoopoeConfig.expandDirectPaths) {
      _expandInvocation(invocation);
      if (invocation.children && invocation.children.length == 1) {
        _expandDirectPaths(invocation.children[0]);
      }
    }
  }

  function _expandInvocationsWithAttributes(invocation) {
    if (invocation.attributes && invocation.attributes.length > 0) {
      _expandInvocation(invocation);
    }
    if (invocation.children) {
      invocation.children.forEach(function (childInvocation) {
        _expandInvocationsWithAttributes(childInvocation);
      });
    }
  }

  function _enrichInvocationsData(invocation) {
    if (invocation.children) {
      invocation.children.forEach(function (childInvocation) {
        childInvocation.parent = invocation;
        _enrichInvocationsData(childInvocation);
      });
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
        _enrichInvocationsData($controller.data);
        _expandDirectPaths($controller.data);
        // todo this should be a button
        _expandInvocationsWithAttributes($controller.data);
      },
      function () {
        //todo handle errors
      }
    );

}