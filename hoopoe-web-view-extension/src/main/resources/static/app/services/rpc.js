function ProfilerRpc(jsonrpc, errorHelper, $q) {
  return {
    startProfiling: function () {
      return $q(function (resolve, reject) {
        jsonrpc.request('profiler', 'startProfiling', {})
          .then(function () {
            resolve();
          })
          .catch(errorHelper.rpcErrorHandler(reject));
      });
    },

    stopProfiling: function () {
      return $q(function (resolve, reject) {
        jsonrpc.request('profiler', 'stopProfiling', {})
          .then(function (profiledResult) {
            resolve(profiledResult);
          })
          .catch(errorHelper.rpcErrorHandler(reject));
      });
    },

    isProfiling: function () {
      return $q(function (resolve, reject) {
        jsonrpc.request('profiler', 'isProfiling', {})
          .then(function (isProfiling) {
            resolve(isProfiling);
          })
          .catch(errorHelper.rpcErrorHandler(reject));
      });
    },

    getLastProfiledResult: function () {
      return $q(function (resolve, reject) {
        jsonrpc.request('profiler', 'getLastProfiledResult', {})
          .then(function (profiledResult) {
            resolve(profiledResult);
          })
          .catch(errorHelper.rpcErrorHandler(reject));
      });
    }

  }
}