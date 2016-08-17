function HelperService() {  //todo rename and probably separate

  var numberFormats = [];

  function _printNumber(value, decimals) {
    if (!decimals) {
      decimals = 3;
    }
    var numberFormat = numberFormats[decimals];
    if (!numberFormat) {
      numberFormat = wNumb({
        mark: decimals == 0 ? false : '.',
        thousand: ' ',
        decimals: decimals
      });
      numberFormats[decimals] = numberFormat;
    }

    return numberFormat.to(value);
  }

  function _getSmartDuration(durationInNs) {
    var durationInMs = durationInNs / 1000000;
    if (durationInMs < 0.1) {
      return '<0.1ms';
    }
    else {
      var durationInSec = durationInMs / 1000;
      if (durationInSec < 0.2) {
        return _printNumber(durationInMs, 1) + 'ms'
      }
      else {
        return _printNumber(durationInSec, 2) + 's'
      }
    }
  }

  return {
    printFloat: _printNumber,
    getSmartDuration: _getSmartDuration
  };
}

function ErrorHelper() {

  function _handleError(message, details) {
    //todo show growl
    if (details && console && console.log) {
      console.log(details);
    }
  }

  return {
    handleError: _handleError,

    rpcErrorHandler: function (rejectCallback) {
      return function (error) {
        _handleError(error.message, error);
        rejectCallback();
      }
    }
  }

}

function OperationsProgressService() {
  var _currentInProgressOperations = 0;

  return {
    isInProgress: function () {
      return _currentInProgressOperations > 0;
    },

    startOperation: function () {
      _currentInProgressOperations++;
    },

    finishOperation: function () {
      _currentInProgressOperations--;
    }
  }
}