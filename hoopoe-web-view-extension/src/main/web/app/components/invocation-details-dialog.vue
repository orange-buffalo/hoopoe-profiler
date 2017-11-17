<template>
  <hp-dialog :dialog="dialog">
    <v-card>
      <v-toolbar>
        <v-toolbar-title>Call Details</v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon @click.native="dialog.hide()" dark>
          <v-icon>close</v-icon>
        </v-btn>
      </v-toolbar>

      <div class="hp-details-item">
        <div class="hp-tile">Method signature</div>
        <div class="hp-monospaced" v-html="methodSignatureHtml"></div>
        <v-divider></v-divider>
      </div>

      <div class="hp-details-item">
        <div class="hp-tile">Total time / Own Time</div>
        <div class="hp-monospaced">
          <smart-duration :duration-in-ns="invocation.totalTimeInNs"></smart-duration>
          /
          <smart-duration :duration-in-ns="invocation.ownTimeInNs"></smart-duration>
        </div>
        <v-divider></v-divider>
      </div>

      <div class="hp-details-item">
        <div class="hp-tile">Total invocations
          <span v-if="invocation.invocationsCount > 1"> / Avg. total time / Avg. own time</span></div>
        <div class="hp-monospaced">
          <span>{{ invocation.invocationsCount }}</span>
          <span v-if="invocation.invocationsCount > 1">
              /
              <smart-duration :duration-in-ns="invocation.totalTimeInNs / invocation.invocationsCount">
              </smart-duration>
              /
              <smart-duration :duration-in-ns="invocation.ownTimeInNs / invocation.invocationsCount">
              </smart-duration>
          </span>
        </div>
        <v-divider></v-divider>
      </div>

      <div class="hp-details-item" v-for="attribute in invocation.attributes">
        <div class="hp-tile">{{ attribute.name }}</div>
        <div class="hp-monospaced">{{ attribute.details }}</div>
        <v-divider></v-divider>
      </div>

    </v-card>
  </hp-dialog>
</template>

<script>
  import SmartDuration from './smart-duration.vue'
  import HpDialog from './dialog/hp-dialog.vue'
  import HpDialogModel from './dialog/hp-dialog'
  import _ from "lodash"

  export default {
    name: 'invocations-details-dialog',
    props: ['invocation', 'dialog'],
    components: {
      SmartDuration,
      HpDialog
    },
    data: function () {
      return {
      }
    },
    computed: {
      methodSignatureHtml: function () {
        let methodSignature = _.escape(this.invocation.methodSignature);
        let className = _.escape(this.invocation.className);
        if (methodSignature.endsWith('()') || className.length + methodSignature.length < 80) {
          return className + '.' + methodSignature;
        }

        let paramsIndex = methodSignature.indexOf('(');
        let params = methodSignature.substring(paramsIndex + 1, methodSignature.length - 1)
            .split(',');
        let html = '<div class="hp-wrap-words">' + className
            + '.' + methodSignature.substring(0, paramsIndex + 1)
            + '</div>';
        params.forEach(function (param, index) {
          html += '<div class="hp-method-param-indent hp-wrap-words">' + param
              + (index === params.length - 1 ? ')' : ',');
          html += '</div>';
        });
        return html;
      }
    }

  }
</script>

<style lang="scss">
  .hp-details-item {
    padding: 0 2em;
    margin-top: 1.2em;

    .hp-title {
      font-size: 1.1em;
      margin-bottom: 0.5em;
    }

    hr {
      margin-top: 1.2em;
    }

    .hp-method-param-indent {
      padding-left: 8em;
    }

  }
</style>