import injector from 'vue-inject'

function ProfilerRpc(jsonrpc, jsonrpcConfig) {

  jsonrpcConfig.set({
    servers: [{
      name: 'profiler',
      url: '/rpc/profiler'
    }]
  });

  return {
    startProfiling: function () {
      return jsonrpc.request('profiler', 'startProfiling', {})
    },

    stopProfiling: function () {
      return jsonrpc.request('profiler', 'stopProfiling', {})
    },

    isProfiling: function () {
      return jsonrpc.request('profiler', 'isProfiling', {})
    },

    getLastProfiledResult: function () {
      return jsonrpc.request('profiler', 'getLastProfiledResult', {})
    }
  }
}

injector.factory('profilerRpc', ['jsonrpc', 'jsonrpcConfig'], ProfilerRpc);