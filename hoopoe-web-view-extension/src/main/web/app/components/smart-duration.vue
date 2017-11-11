<template>
  <span>{{ durationStr }}</span>
</template>

<script>
  import wNumb from 'wnumb'

  export default {
    name: 'smart-duration',
    props: ['durationInNs'],
    computed: {
      durationStr: function () {
        function _printNumber(value, decimals) {
          if (!decimals) {
            decimals = 3;
          }
          return wNumb({
            mark: decimals === 0 ? false : '.',
            thousand: ' ',
            decimals: decimals
          }).to(value);
        }

        let durationInMs = this.durationInNs / 1000000;
        if (durationInMs < 0.1) {
          return '<0.1ms';
        }
        else {
          let durationInSec = durationInMs / 1000;
          if (durationInSec < 0.2) {
            return _printNumber(durationInMs, 1) + 'ms'
          }
          else {
            return _printNumber(durationInSec, 2) + 's'
          }
        }
      }
    }
  }
</script>