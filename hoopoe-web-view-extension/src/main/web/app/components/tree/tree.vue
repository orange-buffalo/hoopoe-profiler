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
      },
    }
  }
</script>

<style lang="scss">
  @import "~compass-mixins/lib/compass";

  .hp-tree {
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

  /*.hp-tree li span {*/
  /*outline: none;*/
  /*}*/

  /*treecontrol li.tree-expanded i.tree-branch-head, treecontrol li.tree-collapsed i.tree-branch-head {*/
  /*display: inline;*/
  /*}*/

  /*treecontrol li .hp-tree-handle {*/
  /*cursor: pointer;*/
  /*}*/

  /*treecontrol li .tree-label {*/
  /*cursor: pointer;*/
  /*display: inline;*/
  /*white-space: nowrap;*/
  /*outline: none;*/
  /*}*/

  /*treecontrol li .tree-selected {*/
  /*font-weight: bold;*/
  /*background-color: rgba(255, 255, 255, 0.2)*/
  /*}*/

  /*treecontrol .tree-label:hover {*/
  /*background-color: rgba(255, 255, 255, 0.2)*/
  /*}*/
</style>
