function HelperService() {

  var numberFormats = [];

  function _printTime(value) {
    return moment(new Date(Number(value))).format("DD MMM YY, kk:mm:ss");
  }

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
    getSmartDuration: _getSmartDuration,
    printTime: _printTime
  };
}