<template>
  <v-layout row align-center>
    <v-flex class="text-xs-center">
      <span class="hero-message" v-if="model.message">{{model.message}}</span>
      <span class="hero-message" v-if="model.hasError">
        Oops, something went wrong... We would appreciate if you could report a problem
        at <a href="https://github.com/orange-buffalo/hoopoe-profiler/issues" target="_blank">our issue tracker</a>.
        Please provide the output of JS console and exception from your app console.
      </span>
      <br/>
      <v-btn class="hero-button"
             color="accent"
             v-if="model.buttonText"
             v-on:click="invokeAction">{{model.buttonText}}
      </v-btn>
    </v-flex>
  </v-layout>
</template>

<script>
  export default {
    name: 'hero-panel',
    props: ['model'],
    methods: {
      invokeAction: function () {
        this.$emit('action-invoked')
      }
    }
  }
</script>

<style lang="scss" scoped>
  .hero-button {
    /*padding: 10px;*/
    /*font-size: 130%;*/
  }

  .hero-message {
    margin-bottom: 40px;
    font-size: 22px;
    font-family: 'Source Code Pro', monospace;
    display: inline-block;
  }
</style>
