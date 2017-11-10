class ProfiledInvocations {

  constructor(apiResponse) {
    let roots;
    if (apiResponse && apiResponse.invocations && apiResponse.invocations.length) {
      roots = apiResponse.invocations;
    }
    else {
      roots = [];
    }

    function _addAttributeSummary(invocation, summary) {
      let currentSummary = invocation.childrenAttributesSummary[summary.name];
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

        for (let key in childInvocation.childrenAttributesSummary) {
          _addAttributeSummary(invocation, childInvocation.childrenAttributesSummary[key]);
        }
      });
    }

    this.roots = roots.map((invocationRoot) => {
      _enrichInvocationsData(invocationRoot.invocation);
      invocationRoot.invocation.threadName = invocationRoot.threadName;
      return invocationRoot.invocation;
    });

    this.roots.sort(function (a, b) {
      return b.totalTimeInNs - a.totalTimeInNs;
    });
  }

  static empty() {
    return new ProfiledInvocations(null);
  }

  isEmpty() {
    return this.roots.length === 0;
  }

}

export default ProfiledInvocations;