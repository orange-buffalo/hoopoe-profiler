<template>
  <tree :root-nodes="treeNodes">
    <template slot="node-content" slot-scope="{ data: invocation }">

      <span class="hp-badge hp-thread-name"
            v-if="invocation.threadName">{{invocation.threadName}}</span>
      <span>{{invocation.className}}.{{invocation.methodSignature}}</span>

      <smart-duration class="hp-badge hp-duration" :duration-in-ns="invocation.totalTimeInNs"></smart-duration>

      <smart-duration class="hp-badge hp-duration" :duration-in-ns="invocation.ownTimeInNs"></smart-duration>

      <span class="hp-badge hp-invocations-count">{{ invocation.invocationsCount }} inv</span>

      <span class="hp-badge hp-attribute"
            v-for="attr in invocation.attributes">
          {{attr.name}}
      </span>
      <span class="hp-badge hp-child-attribute"
            v-for="summary in invocation.childrenAttributesSummary">
          {{summary.count}} {{summary.name}}
      </span>

    </template>
  </tree>
</template>

<script>
  import Tree from './tree/tree.vue'
  import TreeNode from './tree/tree-node'
  import SmartDuration from './smart-duration.vue'

  export default {
    name: 'invocations-tree',
    props: ['invocations'],
    data: function () {
      return {
        treeNodes: []
      }
    },
    components: {
      Tree,
      SmartDuration
    },
    created: function () {
      this.treeNodes = TreeNode.of(this.invocations);
    }
  }
</script>

<style lang="scss" scoped>
  @import "~compass-mixins/lib/compass";

  .hp-badge {
    display: inline-block;
    padding: 1px 3px;
    @include box-sizing(border-box);
    line-height: 13px;
    color: #202525;
    border-radius: 1px;
  }

  .hp-thread-name {
    background-color: #727874;
  }

  .hp-duration {
    background-color: #698788;
  }

  .hp-invocations-count {
    background-color: #6c8672;
  }

  .hp-attribute {
    background-color: #c4b34b;
  }

  .hp-child-attribute {
    background-color: #777c75;
  }
</style>
