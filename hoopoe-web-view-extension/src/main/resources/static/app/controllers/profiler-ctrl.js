function ProfilerCtrl(profilerRpc, operationsProgressService, $scope, $mdDialog, helperService) {
  var ProfilingState = {
    INITIALIZING: 0,
    NOT_YET_PROFILED: 1,
    IN_PROGRESS: 2,
    FINALIZING: 3,
    NOTHING_PROFILED: 4,
    PROFILED: 5,
    ERROR: 6
  };

  var SearchType = {
    ATTRIBUTES: 0
  };

  var $controller = this;

  var _profilingState = ProfilingState.INITIALIZING;

  function _addAttributeSummary(invocation, summary) {
    var currentSummary = invocation.childrenAttributesSummary[summary.name];
    if (currentSummary) {
      currentSummary.count += summary.count;
    }
    else {
      invocation.childrenAttributesSummary[summary.name] = {
        name: summary.name,
        count: summary.count
      };
    }
  }

  function _enrichInvocationsData(invocation) {
    invocation.childrenAttributesSummary = {};
    if (!invocation.children) {
      invocation.children = [];
    }
    if (!invocation.attributes) {
      invocation.attributes = [];
    }

    invocation.children.forEach(function (childInvocation) {
      childInvocation.parent = invocation;

      _enrichInvocationsData(childInvocation);

      childInvocation.attributes.forEach(function (childAttribute) {
        _addAttributeSummary(invocation, {
          name: childAttribute.name,
          count: 1
        });
      });

      for (var key in childInvocation.childrenAttributesSummary) {
        _addAttributeSummary(invocation, childInvocation.childrenAttributesSummary[key]);
      }
    });
  }

  function _consumeProfiledResult(profiledResult) {
    if (profiledResult.invocations.length == 0) {
      _profilingState = ProfilingState.NOTHING_PROFILED;
    }
    else {
      profiledResult.invocations.forEach(function (invocationRoot) {
        _enrichInvocationsData(invocationRoot.invocation);
        invocationRoot.invocation.threadName = invocationRoot.threadName;
        $controller.invocationsTree.rootNodes.push(invocationRoot.invocation);
        $controller.invocationsTree.sortRootNodes();
      });

      _profilingState = ProfilingState.PROFILED;
    }
    operationsProgressService.finishOperation();
  }

  function _resetInvocationsTree() {
    $controller.invocationsTree = new InvocationsTree($scope.hoopoeConfig.expandDirectPaths);
  }

  _resetInvocationsTree();

  $controller.searchAttributes = function () {
    $controller.currentSearch = new InvocationTreeSearch(
      $controller.invocationsTree,
      SearchType.ATTRIBUTES,
      function (invocation) {
        return invocation.attributes.length > 0;
      }
    );
  };

  $controller.isSearchingAttributes = function () {
    return $controller.currentSearch && $controller.currentSearch.type === SearchType.ATTRIBUTES;
  };

  $controller.isSearchInProgress = function () {
    return $controller.currentSearch;
  };

  $controller.cancelSearch = function () {
    $controller.currentSearch = null;
  };

  $controller.showMethodDetailsPopup = function () {
    $mdDialog.show({
      templateUrl: 'views/method-details.html',
      clickOutsideToClose: true,
      controller: function ($scope, $mdDialog) {
        $scope.invocation = $controller.invocationsTree.selectedNode;
        $scope.helperService = helperService;

        $scope.closeDialog = function () {
          $mdDialog.hide();
        }
      }
    });
  };

  $controller.startProfiling = function () {
    operationsProgressService.startOperation();

    _resetInvocationsTree();

    profilerRpc.startProfiling().then(
      function () {
        _profilingState = ProfilingState.IN_PROGRESS;
      },
      function () {
        operationsProgressService.finishOperation();
        _profilingState = ProfilingState.ERROR;
      }
    )
  };

  $controller.stopProfiling = function () {
    _profilingState = ProfilingState.FINALIZING;
    profilerRpc.stopProfiling().then(
      function (profiledResult) {
        _consumeProfiledResult(profiledResult);
      },
      function () {
        operationsProgressService.finishOperation();
        _profilingState = ProfilingState.ERROR;
      }
    )
  };

  $controller.isInitializing = function () {
    return _profilingState == ProfilingState.INITIALIZING;
  };

  $controller.isNotYetProfiled = function () {
    return _profilingState == ProfilingState.NOT_YET_PROFILED;
  };

  $controller.isInProgress = function () {
    return _profilingState == ProfilingState.IN_PROGRESS;
  };

  $controller.isFinalizing = function () {
    return _profilingState == ProfilingState.FINALIZING;
  };

  $controller.isProfiled = function () {
    return _profilingState == ProfilingState.PROFILED;
  };

  $controller.isError = function () {
    return _profilingState == ProfilingState.ERROR;
  };

  $controller.isNothingProfiled = function () {
    return _profilingState == ProfilingState.NOTHING_PROFILED;
  };

  $controller.startOver = function () {
    var confirm = $mdDialog.confirm()
      .title('Are you sure?')
      .textContent('This action will discard current profiled data. It will not be possible to restore it. ' +
        'Are you sure you want to continue and start new profiling?')
      .ok('Do it!')
      .cancel('No.. not now');
    $mdDialog.show(confirm).then(function () {
      $controller.startProfiling();
    });
  };


  operationsProgressService.startOperation();

  profilerRpc.isProfiling().then(
    function (profiling) {
      if (profiling) {
        _profilingState = ProfilingState.IN_PROGRESS;
      }
      else {
        profilerRpc.getLastProfiledResult().then(
          function (profiledResult) {
            if (profiledResult && profiledResult.invocations.length > 0) {
              _consumeProfiledResult(profiledResult);
            }
            else {
              operationsProgressService.finishOperation();
              _profilingState = ProfilingState.NOT_YET_PROFILED;
            }

          },
          function () {
            operationsProgressService.finishOperation();
            _profilingState = ProfilingState.ERROR;
          }
        )
      }
    },
    function () {
      operationsProgressService.finishOperation();
      _profilingState = ProfilingState.ERROR;
    }
  );

}