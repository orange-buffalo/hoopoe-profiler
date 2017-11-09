<template>
  <v-app dark>
    <v-toolbar app flat>
      <v-progress-linear v-bind:indeterminate="true"
                         v-if="apiCallInProgress"
                         class="hp-progress-bar"
                         height="2"></v-progress-linear>
    </v-toolbar>

    <main>
      <v-content>
        <v-container fluid fill-height>
          <hero-panel v-if="apiCallInProgress"
                      :message="heroMessage"
                      :button-text="heroButtonText"
                      v-on:action-invoked="heroButtonAction"></hero-panel>
        </v-container>
      </v-content>
    </main>
  </v-app>
</template>

<script>
  import HeroPanel from './components/HeroPanel.vue'

  export default {
    name: 'app',
    dependencies: ['profilerRpc'],
    data() {
      return {
        apiCallInProgress: true,
        heroMessage: 'Initializing...',
        heroButtonText: '',
        heroButtonAction: () => null,
      }
    },
    components: {
      HeroPanel
    },
    methods: {
      _setupProfilingInProgress: function () {
        this.heroMessage = 'We are recording application activity. Stop us when you are done';
        this.heroButtonText = 'Finish profiling';
        this.apiCallInProgress = true;
      },

      _setupProfiledResult: function (profiledResult) {
        this.heroMessage = 'Profiled';
        this.heroButtonText = null;
        this.apiCallInProgress = true;
        console.log(profiledResult);
      },

      _executeRpc: function (rpcCall) {
        this.heroMessage = 'Processing...';
        this.apiCallInProgress = true;

        return rpcCall().then(result => new Promise(resolve => {
          resolve(result);
          this.apiCallInProgress = false;

        })).catch(error => {
          this.heroMessage = "OOOps"

        })
      },

    },
    created() {
      this._executeRpc(this.profilerRpc.isProfiling)
          .then(profiling => {
            if (profiling) {
              this._setupProfilingInProgress();
            }
            else {
              this._executeRpc(this.profilerRpc.getLastProfiledResult)
                  .then(this._setupProfiledResult)
            }
          });
    }
  }
</script>

<style lang="scss">
  .hp-progress-bar {
    position: absolute;
    left: 0;
    bottom: 0;
    right: 0;
    margin: 0 !important;
  }
</style>
