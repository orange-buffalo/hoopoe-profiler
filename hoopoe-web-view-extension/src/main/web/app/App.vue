<template>
  <v-app dark>
    <v-toolbar app flat>
      <v-layout row align-center>
        <v-flex class="text-xs-left">
          <v-btn flat v-if="!apiCallInProgress && profiledInvocations"
                 @click="startNewSession">New Profiling Session
          </v-btn>
        </v-flex>
      </v-layout>
      <v-progress-linear :indeterminate="true"
                         v-if="apiCallInProgress"
                         class="hp-progress-bar"
                         height="2"></v-progress-linear>
    </v-toolbar>

    <main>
      <v-content>
        <v-container fluid fill-height>
          <hero-panel :message="heroMessage"
                      :button-text="heroButtonText"
                      :error="heroError"
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
        heroError: false,
        heroButtonText: null,
        heroButtonAction: () => null,
        profiledInvocations: null
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
        this.heroButtonAction = () => {
          this.heroMessage = 'Finalizing..';
          this.heroButtonText = null;
          this._executeRpc(this.profilerRpc.stopProfiling).then(this._setupProfiledResult);
        }
      },

      _setupProfiledResult: function (profiledResult) {
        this.profiledInvocations = profiledResult;
        this.apiCallInProgress = false;
        console.log(profiledResult);
      },

      _executeRpc: function (rpcCall) {
        let resetHeroAndLoader = () => {
          this.heroMessage = null;
          this.heroError = false;
          this.apiCallInProgress = false;
        };

        this.heroMessage = 'Processing...';
        this.apiCallInProgress = true;
        this.profiledInvocations = null;
        this.heroError = false;

        return new Promise(resolve => {
          rpcCall().then(result => {
            resolve(result);
            resetHeroAndLoader();

          }).catch(error => {
            resetHeroAndLoader();
            this.heroError = true;

            console.error(error);
          });
        });
      },

      startNewSession: function () {
        this._executeRpc(this.profilerRpc.startProfiling)
            .then(this._setupProfilingInProgress)
      }

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
