<template>
  <v-app dark>
    <v-toolbar app flat>
      <v-layout row align-center>
        <v-flex class="text-xs-left">
          <v-btn flat v-if="!apiCallInProgress && !heroModel.isVisible()"
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
  import HeroPanel from './components/hero-panel.vue'
  import HeroModel from './components/hero-model'
  import ProfiledInvocations from './domain/profiled-invocations'
  import _ from 'lodash'

  export default {
    name: 'app',
    dependencies: ['profilerRpc'],
    data() {
      return {
        apiCallInProgress: true,
        heroModel: HeroModel.forMessage('Initializing...'),
        heroButtonAction: () => null,
        profiledInvocations: ProfiledInvocations.empty()
      }
    },
    components: {
      HeroPanel
    },
    methods: {
      _setupProfilingInProgress: function () {
        this._setHeroModel(HeroModel.forMessage('We are recording application activity. Stop us when you are done')
            .withButton('Finish profiling'));
        this.apiCallInProgress = true;
        this.heroButtonAction = this.stopProfiling;
      },

      _setupProfiledResult: function (apiResponse, firstRun) {
        this.apiCallInProgress = false;
        this._setHeroModel(HeroModel.empty());
        this.profiledInvocations = new ProfiledInvocations(apiResponse);

        if (this.profiledInvocations.isEmpty()) {
          if (firstRun) {
            this._setHeroModel(HeroModel.forMessage('We haven\'t profiled anything yet')
                .withButton('Start profiling'))
          }
          else {
            this._setHeroModel(HeroModel.forMessage(
                'It looks like nothing is profiled, methods were either too fast or not called..')
                .withButton('Start anew'));
          }
          this.heroButtonAction = this.startNewSession;
        }
      },

      _executeRpc: function (rpcCall) {
        this._setHeroModel(HeroModel.forMessage('Processing, wait a sec...'));
        this.apiCallInProgress = true;

        return new Promise(resolve => {
          rpcCall().then(result => {
            resolve(result);
            this.apiCallInProgress = false;

          }).catch(error => {
            this._setHeroModel(HeroModel.error());

            console.error(error);
          });
        });
      },

      stopProfiling: function () {
        this._executeRpc(this.profilerRpc.stopProfiling)
            .then(apiResponse => this._setupProfiledResult(apiResponse, false));
      },

      startNewSession: function () {
        this.profiledInvocations = ProfiledInvocations.empty();
        this._executeRpc(this.profilerRpc.startProfiling)
            .then(this._setupProfilingInProgress)
      }

    },
    created() {
      this._setHeroModel = _.debounce((model) => this.heroModel = model, 200);

      this._executeRpc(this.profilerRpc.isProfiling)
          .then(profiling => {
            if (profiling) {
              this._setupProfilingInProgress();
            }
            else {
              this._executeRpc(this.profilerRpc.getLastProfiledResult)
                  .then(apiResponse => this._setupProfiledResult(apiResponse, true))
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
