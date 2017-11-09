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
          <hero-panel :model="heroModel"
                      v-on:action-invoked="heroButtonAction"></hero-panel>
        </v-container>
      </v-content>
    </main>
  </v-app>
</template>

<script>
  import HeroPanel from './components/HeroPanel.vue'
  import {HeroModel} from './components/hero-model'

  export default {
    name: 'app',
    dependencies: ['profilerRpc'],
    data() {
      return {
        apiCallInProgress: true,
        heroModel: HeroModel.forMessage('Initializing...'),
        heroButtonAction: () => null,
        profiledInvocations: null
      }
    },
    components: {
      HeroPanel
    },
    methods: {
      _setupProfilingInProgress: function () {
        this.heroModel = HeroModel.forMessage('We are recording application activity. Stop us when you are done')
            .withButton('Finish profiling');
        this.apiCallInProgress = true;
        this.heroButtonAction = this.stopProfiling;
      },

      _setupProfiledResult: function (profiledResult) {
        this.profiledInvocations = profiledResult;
        this.apiCallInProgress = false;
        this.heroModel = HeroModel.empty();

        console.log(profiledResult);

        if (!profiledResult || !profiledResult.invocations || !profiledResult.invocations.length) {
          this.heroModel = HeroModel.forMessage(
              'It looks like nothing is profiled, methods were either too fast or not called..')
              .withButton('Start anew');
          this.heroButtonAction = this.startNewSession;
        }
      },

      _executeRpc: function (rpcCall) {
        this.heroModel = HeroModel.forMessage('Processing, wait a sec...');
        this.apiCallInProgress = true;

        return new Promise(resolve => {
          rpcCall().then(result => {
            resolve(result);
            this.apiCallInProgress = false;

          }).catch(error => {
            this.heroModel = HeroModel.error();

            console.error(error);
          });
        });
      },

      stopProfiling: function () {
        this._executeRpc(this.profilerRpc.stopProfiling).then(this._setupProfiledResult);
      },

      startNewSession: function () {
        this.profiledInvocations = null;
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
