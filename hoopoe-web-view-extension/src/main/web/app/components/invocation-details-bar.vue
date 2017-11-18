<template>
  <transition name="fade" :duration="100" slot="container-content">
    <v-snackbar
        fixed
        left
        v-if="invocation"
        :timeout="0"
        v-model="invocation">

      Own Time:
      <smart-duration :duration-in-ns="invocation.ownTimeInNs" class="hp-badge hp-duration"></smart-duration>
      Total Time:
      <smart-duration :duration-in-ns="invocation.totalTimeInNs" class="hp-badge hp-duration"></smart-duration>

      <v-btn flat icon @click="expandCollapse()">
        <v-icon>{{ invocation.$treeNode.expanded ? 'fa-compress' : 'fa-expand' }}</v-icon>
      </v-btn>

      <v-btn flat @click="dialog.show()">Details</v-btn>

      <invocation-details-dialog :dialog="dialog" :invocation="invocation"></invocation-details-dialog>
    </v-snackbar>
  </transition>
</template>

<script>
  import SmartDuration from './smart-duration.vue'
  import HpDialogModel from './dialog/hp-dialog'
  import InvocationDetailsDialog from './invocation-details-dialog.vue'

  export default {
    name: 'invocations-details-bar',
    props: ['invocation'],
    components: {
      SmartDuration,
      InvocationDetailsDialog
    },
    data: function () {
      return {
        dialog: new HpDialogModel()
      }
    },
    methods: {
      expandCollapse: function () {
        if (this.invocation.$treeNode.expanded) {
          this.invocation.$treeNode.collapse();
        }
        else {
          this.invocation.$treeNode.expandAll();
        }
      }
    }

  }
</script>

<style lang="scss" scoped>

  .snack {
    bottom: 70px;
  }

  .hp-badge {
    margin: 0 0.3em;
  }

  .icon {
    font-size: 15px;
  }
</style>