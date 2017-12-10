<template>
  <v-app dark class="hp-full-height">
    <v-toolbar app flat fixed>
      <v-toolbar-items>
        <hp-roots-selector v-if="!apiCallInProgress && !heroModel.isVisible()"
                           :roots="treeRoots">
        </hp-roots-selector>

        <v-btn v-if="!apiCallInProgress && !heroModel.isVisible()"
               flat
               @click="switchHotSpots">
          {{ hotSpotsVisible ? 'Invocations' : 'Hot Spots' }}
        </v-btn>
      </v-toolbar-items>
      <v-progress-linear :indeterminate="true"
                         v-if="apiCallInProgress"
                         class="hp-progress-bar"
                         height="2"></v-progress-linear>
    </v-toolbar>

    <v-btn
        v-if="!apiCallInProgress && !heroModel.isVisible()"
        fab
        bottom
        right
        fixed
        color="accent"
        @click="startNewSession">
      <v-icon>replay</v-icon>
    </v-btn>

    <main class="hp-full-height">
      <v-content class="scroll-y hp-main-content">
        <v-container fluid fill-height>
          <transition name="slideLeft" :duration="20" mode="out-in">
            <hero-panel :model="heroModel"
                        v-if="heroModel.isVisible()"
                        v-on:action-invoked="heroButtonAction"></hero-panel>

            <invocations-tree :invocations="visibleRoots"
                              v-if="!heroModel.isVisible()"></invocations-tree>
          </transition>
        </v-container>
      </v-content>
    </main>
  </v-app>
</template>

<script>
  import HeroPanel from './components/hero-panel/hero-panel.vue'
  import HeroModel from './components/hero-panel/hero-model'
  import InvocationsTree from './components/invocations-tree.vue'
  import ProfiledInvocations from './domain/profiled-invocations'
  import HpRootsSelector from './components/hp-roots-selector.vue'
  import _ from 'lodash'

  export default {
    name: 'app',
    dependencies: ['profilerRpc'],
    data() {
      return {
        apiCallInProgress: true,
        heroModel: HeroModel.forMessage('Initializing...'),
        heroButtonAction: () => null,
        profiledInvocations: ProfiledInvocations.empty(),
        hotSpotsVisible: false,
        hotSpots: ProfiledInvocations.empty(),
        hotSpotsRequested: false
      }
    },
    components: {
      HeroPanel,
      InvocationsTree,
      HpRootsSelector
    },
    computed: {
      treeRoots: function () {
         return this.hotSpotsVisible ? this.hotSpots.roots : this.profiledInvocations.roots;
      },

      visibleRoots: function () {
        return this.treeRoots.filter(root => root.visible);
      }
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
        this.hotSpots = ProfiledInvocations.empty();
        this.hotSpotsRequested = false;

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
        else {
          this.profiledInvocations.roots.forEach(root => {
            // enable reactivity on new property
            this.$set(root, 'visible', false);
          });
          this.profiledInvocations.roots[0].visible = true;
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
            this.apiCallInProgress = false;
            this._setHeroModel(HeroModel.error());

            console.error(error);
          });
        });
      },

      stopProfiling: function () {
        this._executeRpc(() => this.profilerRpc.stopProfiling())
            .then(apiResponse => this._setupProfiledResult(apiResponse, false));
      },

      startNewSession: function () {
        this.profiledInvocations = ProfiledInvocations.empty();
        this.hotSpots = ProfiledInvocations.empty();
        this.hotSpotsVisible = false;
        this.hotSpotsRequested = false;
        this._executeRpc(() => this.profilerRpc.startProfiling())
            .then(this._setupProfilingInProgress)
      },

      switchHotSpots: function () {
        this.hotSpotsVisible = !this.hotSpotsVisible;
        if (this.hotSpotsVisible && !this.hotSpotsRequested) {
          this._executeRpc(() => this.profilerRpc.calculateHotSpots(5))
              .then(apiResponse => {
                this.hotSpots = new ProfiledInvocations(apiResponse);
                this.hotSpots.roots.forEach(root => {
                  // enable reactivity on new property
                  this.$set(root, 'visible', true);
                });
                this.hotSpotsRequested = true;
                this._setHeroModel(HeroModel.empty());
              });
        }
      }

    },
    created() {
      this._setHeroModel = _.debounce((model) => this.heroModel = model, 200);

      this._executeRpc(() => this.profilerRpc.isProfiling())
          .then(profiling => {
            if (profiling) {
              this._setupProfilingInProgress();
            }
            else {
              this._executeRpc(() => this.profilerRpc.getLastProfiledResult())
                  .then(apiResponse => this._setupProfiledResult(apiResponse, true))
            }
          });
    }
  }
</script>

<style lang="scss">
  html {
    overflow: auto;
  }

  .hp-monospaced {
    font-family: 'Source Code Pro', monospace;
  }

  .hp-wrap-words {
    word-wrap: break-word;
  }

  .hp-full-height {
    height: 100%;
  }

  .hp-main-content {
    padding: 0 !important;
    margin-top: 64px;
  }

  .hp-progress-bar {
    position: absolute;
    left: 0;
    bottom: 0;
    right: 0;
    margin: 0 !important;
  }

  .hp-small-font {
    font-size: 0.9em;
  }
</style>
