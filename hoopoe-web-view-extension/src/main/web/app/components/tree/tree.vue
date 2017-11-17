<template>
  <v-layout column align-top class="hp-tree">
    <v-flex>
      <ul>
        <tree-node v-for="node in rootNodes"
                   :key="node.id"
                   :node="node"
                   @node-select="selectNode">
          <template slot-scope="{ data }" slot="node-content">
            <slot name="node-content" :data="data"></slot>
          </template>
        </tree-node>
      </ul>
    </v-flex>
  </v-layout>
</template>

<script>
  import TreeNode from './tree-node.vue'

  export default {
    name: 'tree',
    props: ['rootNodes'],
    data: function () {
      return {
        selectedNode: null,
      }
    },
    components: {
      TreeNode
    },
    methods: {
      selectNode: function (node) {
        let prevSelectedNode = this.selectedNode;

        node.toggleSelection();
        if (!node.selected) {
          this.selectedNode = null;
        }
        else {
          if (this.selectedNode) {
            this.selectedNode.toggleSelection();
          }
          this.selectedNode = node
        }

        this.$emit('node-select', this.selectedNode, prevSelectedNode);
      },
    }
  }
</script>

<style lang="scss">
  @import "~compass-mixins/lib/compass";

  .hp-tree {
    position: relative;
    @include user-select(none);

    font-family: 'Source Code Pro', monospace;
    text-decoration: none;

    ul {
      margin: 0;
      padding: 0;
      list-style: none;
      border: none;
    }

    li {
      position: relative;
      padding: 0 0 0 10px;
      line-height: 19px;
      white-space: nowrap;
    }

  }

</style>
