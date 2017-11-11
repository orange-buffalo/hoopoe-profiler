<template>
  <v-layout column align-top class="hp-tree">
    <v-flex>
      <ul>
        <tree-item v-for="node in rootNodes"
                   :key="node.id"
                   :node="node">
          <template slot-scope="{ data }" slot="node-content">
            <slot name="node-content" :data="data"></slot>
          </template>
        </tree-item>
      </ul>
    </v-flex>
  </v-layout>
</template>

<script>
  import TreeItem from './tree-item.vue'

  export default {
    name: 'tree',
    props: ['rootNodes'],
    components: {
      TreeItem
    },
    methods: {
      invokeAction: function () {
        this.$emit('action-invoked')
      }
    }
  }
</script>

<style lang="scss">
  .hp-tree {
    -moz-user-select: -moz-none;
    -khtml-user-select: none;
    -webkit-user-select: none;
    -ms-user-select: none;
    user-select: none;

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
