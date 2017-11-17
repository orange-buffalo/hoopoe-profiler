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
    }

  }
</script>

<style lang="scss" scoped>
  .hp-badge {
    margin: 0 0.3em;
  }
</style>