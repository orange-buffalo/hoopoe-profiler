<template>
  <tree :root-nodes="treeNodes">
    <template slot="node-content" slot-scope="{ data: invocation }">
      <span>{{ invocation.className }}</span>
    </template>
  </tree>
</template>

<script>
  import Tree from './tree/tree.vue'
  import TreeNode from './tree/tree-node'

  export default {
    name: 'invocations-tree',
    props: ['invocations'],
    data: function () {
      return {
        treeNodes: []
      }
    },
    components: {
      Tree
    },
    created: function () {
      this.treeNodes = TreeNode.of(this.invocations);
    }
  }
</script>

<style lang="scss">

</style>
