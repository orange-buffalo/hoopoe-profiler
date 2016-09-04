var InvocationsTree = function (expandDirectPaths) {
  var that = this;

  this.rootNodes = [];

  this.options = {
    templateUrl: 'invocations-tree-template.html',  //todo prefetch it and move to separate file
    allowDeselect: false
  };

  this.expandedNodes = [];

  this.selectedNode = null;

  this.onNodeToggle = function (node, expanded) {
    if (expanded) {
      this.expandDirectPaths(node);
    }
    this.selectedNode = null;
  };

  this.sortRootNodes = function () {
    this.rootNodes.sort(function (a, b) {
      return b.totalTimeInNs - a.totalTimeInNs;
    })
  };

  this.expandNode = function (node) {
    if (node && this.expandedNodes.indexOf(node) == -1) {
      this.expandedNodes.push(node);
      this.expandNode(node.parent);
    }
  };

  this.expandDirectPaths = function (node) {
    if (expandDirectPaths) {
      this.expandNode(node);
      if (node.children.length == 1) {
        this.expandDirectPaths(node.children[0]);
      }
    }
  };

  this.getInvocationsCountByPredicate = function (predicate, invocation) {
    var count = 0;
    if (!invocation) {
      this.rootNodes.forEach(function (invocation) {
        count += that.getInvocationsCountByPredicate(predicate, invocation);
      })
    }
    else {
      if (predicate(invocation)) {
        count++;
      }
      invocation.children.forEach(function (childInvocation) {
        count += that.getInvocationsCountByPredicate(predicate, childInvocation);
      })
    }
    return count;
  }

};

var InvocationTreeSearch = function (invocationsTree, type, searchPredicate) {

  this.type = type;

  this.totalFound = invocationsTree.getInvocationsCountByPredicate(searchPredicate);

  this.currentPosition = 0;

  this.next = function () {
    if (this.totalFound === 0) {
      return;
    }
    invocationsTree.selectedNode = _findNextInvocation(invocationsTree.selectedNode);

    this.currentPosition = _calculateCurrentPosition();

    invocationsTree.expandNode(invocationsTree.selectedNode);
  };

  this.next();

  function _calculateCurrentPosition() {
    var tracker = {
      occurrences: 0
    };
    _calculateOccurrencesTillSelectedNode(invocationsTree.rootNodes, tracker);
    return tracker.occurrences;
  }

  function _calculateOccurrencesTillSelectedNode(nodes, tracker) {
    for (var i = 0; i < nodes.length; i++) {
      var node = nodes[i];
      if (searchPredicate(node)) {
        tracker.occurrences++;
      }
      if (node == invocationsTree.selectedNode) {
        return true;
      }
      if (_calculateOccurrencesTillSelectedNode(node.children, tracker)) {
        return true;
      }
    }
    return false;
  }

  function _findFirstInChildrenHierarchy(node) {
    for (var i = 0; i < node.children.length; i++) {
      var childNode = node.children[i];
      if (searchPredicate(childNode)) {
        return childNode;
      }
      var result = _findFirstInChildrenHierarchy(childNode);
      if (result) {
        return result;
      }
    }
    return null;
  }

  function _findInSiblingsHierarchy(node) {
    if (!node) {
      return null;
    }
    var siblings = node.parent ? node.parent.children : invocationsTree.rootNodes;
    var startingNodeIndex = siblings.indexOf(node);
    for (var i = startingNodeIndex + 1; i < siblings.length; i++) {
      var siblingNode = siblings[i];
      if (searchPredicate(siblingNode)) {
        return siblingNode;
      }
      var searchResultInSiblingsChildrenHierarchy = _findFirstInChildrenHierarchy(siblingNode);
      if (searchResultInSiblingsChildrenHierarchy) {
        return searchResultInSiblingsChildrenHierarchy;
      }
    }
    return _findInSiblingsHierarchy(node.parent);
  }

  function _findNextInvocation(invocationNode) {
    var startingNode = invocationNode ? invocationNode : invocationsTree.rootNodes[0];
    var result = _findFirstInChildrenHierarchy(startingNode);
    if (result) {
      return result;
    }
    result = _findInSiblingsHierarchy(startingNode);
    if (!result && invocationNode) {
      return _findNextInvocation();
    }
    return result;
  }
};