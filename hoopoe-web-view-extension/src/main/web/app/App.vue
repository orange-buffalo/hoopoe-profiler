<template>
  <v-app dark class="hp-bg">
    <v-toolbar app class="hp-toolbar" flat>
      <v-btn flat color="primary">Primary</v-btn>
      <v-btn color="primary" class="">Primary</v-btn>

    </v-toolbar>
    <main>
      <v-content>
        <v-container fluid>
          <router-view></router-view>
        </v-container>
      </v-content>
    </main>
  </v-app>
</template>

<script>
  export default {
    name: 'app',
    data() {
      return {
        msg: 'Welcome to Your Vue.js App'
      }
    }
  }
</script>

<style lang="scss">
  .hp-toolbar {
    background-color: #2e3535 !important;
  }

  .hp-bg {
    background-color: #202525 !important;
  }

  .application--dark .toolbar .btn:not(.btn--disabled) {
    color: #424d4b;
  }
</style>
