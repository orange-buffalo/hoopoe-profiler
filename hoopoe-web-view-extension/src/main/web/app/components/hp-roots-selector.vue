<template>
  <v-menu
      offset-x
      :close-on-content-click="false"
      :nudge-width="200"
      v-model="menu"
      v-if="displayed">
    <v-btn flat slot="activator">{{ menuTitle }}</v-btn>
    <v-card class="hp-monospaced">
      <v-list>
        <v-list-tile>
          <v-list-tile-content>
            <v-list-tile-title>Roots to be displayed</v-list-tile-title>
          </v-list-tile-content>
          <v-list-tile-action>
            <v-layout>
              <v-flex>
                <v-btn icon @click="setAll(true)" class="mr-3">
                  <v-icon>fa-check-square-o</v-icon>
                </v-btn>

              </v-flex>
              <v-flex>
                <v-btn icon @click="setAll(false)">
                  <v-icon>fa-square-o</v-icon>
                </v-btn>
              </v-flex>
            </v-layout>
          </v-list-tile-action>
        </v-list-tile>
      </v-list>
      <v-divider></v-divider>
      <v-list>
        <v-list-tile v-for="(root, index) in roots" :key="index">
          <v-list-tile-action>
            <v-switch v-model="root.visible"></v-switch>
          </v-list-tile-action>
          <v-list-tile-title>
            {{ root.threadName }}
            <smart-duration class="hp-badge hp-duration" :duration-in-ns="root.totalTimeInNs"></smart-duration>
            <span class="hp-badge hp-child-attribute"
                  v-for="summary in root.childrenAttributesSummary">
              {{summary.count}} {{summary.name}}
          </span>
          </v-list-tile-title>
        </v-list-tile>
      </v-list>
    </v-card>
  </v-menu>
</template>

<script>
  import SmartDuration from './smart-duration.vue'

  export default {
    name: 'hp-roots-selector',
    props: ['roots'],
    data: function () {
      return {
        menu: false
      }
    },
    components: {
      SmartDuration
    },
    methods: {
      setAll: function (val) {
        this.roots.forEach(root => root.visible = val)
      }
    },
    computed: {
      displayed: function () {
        return this.roots.length > 1
      },
      menuTitle: function () {
        let visibleRootsCount = this.roots.filter(root => root.visible).length;
        if (visibleRootsCount === this.roots.length) {
          return "All roots visible";
        }
        else if (visibleRootsCount === 0) {
          return "All roots hidden";
        }
        else {
          return visibleRootsCount + " of " + this.roots.length + " roots visible";
        }
      }
    }
  }
</script>