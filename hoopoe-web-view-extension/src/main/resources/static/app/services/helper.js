function HelperService() {

  function printTime(value) {
    return moment(new Date(Number(value))).format("DD MMM YY, kk:mm:ss");
  }

  function printFloat(value) {
    return numbro(value).format('0,0.000');
  }

  function getSmartDuration(durationInNs) {
    var durationInMs = durationInNs / 1000000;
    if (durationInMs < 1) {
      return printFloat(durationInNs) + ' ns'
    }
    else {
      var durationInSec = durationInMs / 1000;
      if (durationInSec < 1) {
        return printFloat(durationInMs) + ' ms'
      }
      else {
        return printFloat(durationInSec) + ' s'
      }
    }
  }

  return {
    printFloat: printFloat,
    getSmartDuration: getSmartDuration,
    printTime: printTime
  };
}