<template>
  <v-app dark>
    <v-toolbar app flat>
      <v-btn flat color="primary">Primary</v-btn>
      <v-btn color="primary">Primary</v-btn>

    </v-toolbar>
    <main>
      <v-content>
        <v-container fluid>
        </v-container>
      </v-content>
    </main>
  </v-app>
</template>

<script>
  export default {
    name: 'app',
    dependencies: ['jsonrpc', 'jsonrpcConfig'],
    data() {
      return {}
    },
    created() {
      this.jsonrpcConfig.set({
        servers: [{
          name: 'profiler',
          url: '/rpc/profiler'
        }]
      });

      this.jsonrpc.request('profiler', 'startProfiling', {})
          .then(function (result) {
            console.log("success " + result)
          })
          .catch((reason) => console.log("problem " + reason));
    }
  }
</script>

<style lang="scss">

</style>
