import injector from 'vue-inject'
import _ from 'lodash'

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

class JsonRpcConfig {
  constructor() {
    this.servers = [];

    let config = this;

    this.set = function (args) {
      if (typeof(args) !== 'object') {
        throw new Error('Argument of "set" must be an object.');
      }

      let allowedKeys = ['url', 'servers'];
      let keys = Object.keys(args);
      keys.forEach(function (key) {
        if (allowedKeys.indexOf(key) < 0) {
          throw new JsonRpcConfigError(
              'Invalid configuration key "' + key + '". Allowed keys are: ' + allowedKeys.join(', '));
        }

        if (key === 'url') {
          config.servers = [{
            name: DEFAULT_SERVER_NAME,
            url: args[key],
            headers: {}
          }];
        }
        else if (key === 'servers') {
          config.servers = _getServers(args[key]);
        }
        else {
          config[key] = args[key];
        }
      });
    };

    function _getServers(data) {
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
  }
}

injector.service('jsonrpcConfig', JsonRpcConfig);

class JsonRpc {
  constructor($http, jsonrpcConfig) {
    this.ERROR_TYPE_SERVER = ERROR_TYPE_SERVER;
    this.ERROR_TYPE_TRANSPORT = ERROR_TYPE_TRANSPORT;
    this.ERROR_TYPE_CONFIG = ERROR_TYPE_CONFIG;

    this.JsonRpcTransportError = JsonRpcTransportError;
    this.JsonRpcServerError = JsonRpcServerError;
    this.JsonRpcConfigError = JsonRpcConfigError;

    this.extraHeaders = [];
    let jsonRpc = this;

    let id = 0;

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
      let extra = jsonRpc.extraHeaders[serverName] ? jsonRpc.extraHeaders[serverName] : {};
      const server = _findServer(serverName);
      const headers = _.extend(server.headers, extra);
      return _.extend(headers, DEFAULT_HEADERS);
    }

    function _determineErrorDetails(data, status, url) {
      let errorType = ERROR_TYPE_TRANSPORT;
      let errorMessage;

      if (status === 0) {
        errorMessage = 'Connection refused at ' + url;
      }
      else if (status === 404) {
        errorMessage = '404 not found at ' + url;
      }
      else if (status === 500) {
        if (data.jsonrpc && data.jsonrpc === '2.0') {
          errorType = ERROR_TYPE_SERVER;
          errorMessage = data.error;
        }
        else {
          errorMessage = '500 internal server error at ' + url + ': ' + data;
        }
      }
      else if (status === -1) {
        errorMessage = 'Timeout or cancelled';
      }
      else {
        errorMessage = 'Unknown error. HTTP status: ' + status + ', data: ' + data;
      }

      return {
        type: errorType,
        message: errorMessage,
      };
    }

    this.request = function (arg1, arg2, arg3) {
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

      return promise.then(function (response) {
        if (!response.data) {
          return Promise.reject('Unknown error');
        }
        else if (response.data.result !== undefined) {
          return Promise.resolve(response.data.result);
        }
        else {
          return Promise.reject(new JsonRpcServerError(response.data.error));
        }
      }).catch(function (error) {
        if (error.response) {
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
    };

    this.setHeaders = function (serverName, headers) {
      let server = _findServer(serverName);
      this.extraHeaders[server.name] = headers;
    }
  }
}

injector.service('jsonrpc', ['$http', 'jsonrpcConfig'], JsonRpc);