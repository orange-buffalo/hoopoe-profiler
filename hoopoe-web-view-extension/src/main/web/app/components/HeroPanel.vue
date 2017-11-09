<template>
  <v-layout row align-center>
    <v-flex class="text-xs-center">
      <span class="hero-message">{{message}}</span>
      <br/>
      <v-btn class="hero-button"
             color="accent"
             v-if="buttonText"
             v-on:click="invokeAction">{{buttonText}}
      </v-btn>
    </v-flex>
  </v-layout>
</template>

<script>
  export default {
    name: 'hero-panel',
    props: ['message', 'buttonText'],
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
    color: rgba(138, 138, 138, 0.4);
    font-size: 22px;
    font-family: 'Source Code Pro', monospace;
    display: inline-block;
  }
</style>
