import injector from 'vue-inject'
import _ from 'lodash'

(function () {
  'use strict';

  let id = 0;
  const ERROR_TYPE_SERVER = 'JsonRpcServerError';
  const ERROR_TYPE_TRANSPORT = 'JsonRpcTransportError';
  const ERROR_TYPE_CONFIG = 'JsonRpcConfigError';
  const DEFAULT_SERVER_NAME = 'main';
  const DEFAULT_HEADERS = {
    'Content-Type': 'application/json',
  };

  function JsonRpcTransportError(error) {
    this.name = ERROR_TYPE_TRANSPORT;
    this.message = error;
  }

  JsonRpcTransportError.prototype = Error.prototype;

  function JsonRpcServerError(error) {
    this.name = ERROR_TYPE_SERVER;
    this.message = error.message;
    this.error = error;
    this.data = error.data;
  }

  JsonRpcServerError.prototype = Error.prototype;

  function JsonRpcConfigError(error) {
    this.name = ERROR_TYPE_CONFIG;
    this.message = error;
  }

  JsonRpcConfigError.prototype = Error.prototype;

  function jsonrpc($http, jsonrpcConfig) {
    let extraHeaders = {};

    return {
      request: request,
      setHeaders: setHeaders,
      ERROR_TYPE_SERVER: ERROR_TYPE_SERVER,
      ERROR_TYPE_TRANSPORT: ERROR_TYPE_TRANSPORT,
      ERROR_TYPE_CONFIG: ERROR_TYPE_CONFIG,
      JsonRpcTransportError: JsonRpcTransportError,
      JsonRpcServerError: JsonRpcServerError,
      JsonRpcConfigError: JsonRpcConfigError
    };

    function _getInputData(methodName, args) {
      id += 1;
      return {
        jsonrpc: '2.0',
        id: id,
        method: methodName,
        params: args
      }
    }

    function _findServer(serverName) {
      if (jsonrpcConfig.servers.length === 0) {
        throw new JsonRpcConfigError('Please configure the jsonrpc client first.');
      }

      let servers = jsonrpcConfig.servers.filter(function (s) {
        return s.name === serverName;
      });

      if (servers.length === 0) {
        throw new JsonRpcConfigError('Server "' + serverName + '" has not been configured.');
      }

      return servers[0];
    }

    function _determineArguments(args) {
      if (typeof(args[0]) === 'object') {
        return args[0];
      }
      else if (args.length === 2) {
        return {
          serverName: DEFAULT_SERVER_NAME,
          methodName: args[0],
          methodArgs: args[1],
        };
      }
      else {
        return {
          serverName: args[0],
          methodName: args[1],
          methodArgs: args[2],
        };
      }
    }

    function _determineHeaders(serverName) {
      let extra = extraHeaders[serverName] ? extraHeaders[serverName] : {};
      const server = _findServer(serverName);
      const headers = _.extend(server.headers, extra);
      return _.extend(headers, DEFAULT_HEADERS);
    }

    function _determineErrorDetails(data, status, url) {
      // 2. Call was received by the server. Server returned an error.
      // 3. Call did not arrive at the server.
      let errorType = ERROR_TYPE_TRANSPORT;
      let errorMessage;

      if (status === 0) {
        // Situation 3
        errorMessage = 'Connection refused at ' + url;
      }
      else if (status === 404) {
        // Situation 3
        errorMessage = '404 not found at ' + url;
      }
      else if (status === 500) {
        // This could be either 2 or 3. We have to look at the returned data
        // to determine which one.
        if (data.jsonrpc && data.jsonrpc === '2.0') {
          // Situation 2
          errorType = ERROR_TYPE_SERVER;
          errorMessage = data.error;
        }
        else {
          // Situation 3
          errorMessage = '500 internal server error at ' + url + ': ' + data;
        }
      }
      else if (status === -1) {
        errorMessage = 'Timeout or cancelled';
      }
      else {
        // Situation 3
        errorMessage = 'Unknown error. HTTP status: ' + status + ', data: ' + data;
      }

      return {
        type: errorType,
        message: errorMessage,
      };
    }

    function setHeaders(serverName, headers) {
      let server = _findServer(serverName);
      extraHeaders[server.name] = headers;
    }

    function request(arg1, arg2, arg3) {
      let args = _determineArguments(arguments);

      let server;
      try {
        server = _findServer(args.serverName);
      }
      catch (err) {
        return Promise.reject(err);
      }

      let inputData = _getInputData(args.methodName, args.methodArgs);
      let headers = _determineHeaders(args.serverName);

      let req = {
        method: 'post',
        url: server.url,
        headers: headers,
        data: inputData
      };

      if (args.config) {
        Object.keys(args.config).forEach(function (key) {
          req[key] = args.config[key];
        })
      }

      let promise = $http(req);

      if (jsonrpcConfig.returnHttpPromise) {
        return promise;
      }

      // Here, we determine which situation we are in:
      // 1. Call was a success.
      // 2. Call was received by the server. Server returned an error.
      // 3. Call did not arrive at the server.
      //
      // 2 is a JsonRpcServerError, 3 is a JsonRpcTransportError.
      //
      // We are assuming that the server can use either 200 or 500 as
      // http return code in situation 2. That depends on the server
      // implementation and is not determined by the JSON-RPC spec.
      return promise.then(function (response) {
        // In some cases, it is unfortunately possible to end up in
        // promise.then with data being undefined.
        // This is likely caused either by a bug in the $http service
        // or by incorrect usage of $http interceptors.
        if (!response.data) {
          return Promise.reject(
              'Unknown error, possibly caused by incorrectly configured $http interceptor. ' +
              'See https://github.com/joostvunderink/angular-jsonrpc-client/issues/16 for ' +
              'more information.');
        }
        else if (response.data.result !== undefined) {
          // Situation 1
          return Promise.resolve(response.data.result);
        }
        else {
          // Situation 2
          return Promise.reject(new JsonRpcServerError(response.data.error));
        }
      }).catch(function (error) {
        if (error.response) {
          // Situation 2 or 3.
          const errorDetails = _determineErrorDetails(error.response.data, error.response.status, server.url);

          if (errorDetails.type === ERROR_TYPE_TRANSPORT) {
            return Promise.reject(new JsonRpcTransportError(errorDetails.message));
          }
          else {
            return Promise.reject(new JsonRpcServerError(errorDetails.message));
          }
        } else if (error.request) {
          return Promise.reject(new JsonRpcTransportError("Error while processing request: " + error.request));
        } else {
          return Promise.reject(new JsonRpcTransportError("Error while creating request: " + error.message));
        }
      });
    }
  }

  function jsonrpcConfig() {
    let config = {
      servers: [],
      returnHttpPromise: false
    };

    config.set = function (args) {
      if (typeof(args) !== 'object') {
        throw new Error('Argument of "set" must be an object.');
      }

      let allowedKeys = ['url', 'servers', 'returnHttpPromise'];
      let keys = Object.keys(args);
      keys.forEach(function (key) {
        if (allowedKeys.indexOf(key) < 0) {
          throw new JsonRpcConfigError('Invalid configuration key "' + key + '". Allowed keys are: ' +
              allowedKeys.join(', '));
        }

        if (key === 'url') {
          config.servers = [{
            name: DEFAULT_SERVER_NAME,
            url: args[key],
            headers: {}
          }];
        }
        else if (key === 'servers') {
          config.servers = getServers(args[key]);
        }
        else {
          config[key] = args[key];
        }
      });
    };

    function getServers(data) {
      if (!(data instanceof Array)) {
        throw new JsonRpcConfigError('Argument "servers" must be an array.');
      }
      let servers = [];

      data.forEach(function (d) {
        if (!d.name) {
          throw new JsonRpcConfigError('Item in "servers" argument must contain "name" field.');
        }
        if (!d.url) {
          throw new JsonRpcConfigError('Item in "servers" argument must contain "url" field.');
        }
        let server = {
          name: d.name,
          url: d.url,
        };
        if (d.hasOwnProperty('headers')) {
          server.headers = d.headers;
        }
        else {
          server.headers = {};
        }
        servers.push(server);
      });

      return servers;
    }

    return config;
  }

  injector.constant('jsonrpcConfig', jsonrpcConfig());
  injector.factory('jsonrpc', ['$http', 'jsonrpcConfig'], jsonrpc);
})();
