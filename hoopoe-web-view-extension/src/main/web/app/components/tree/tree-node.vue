<template>
  <li class="hp-tree-node">
    <v-icon class="hp-tree-handle"
            @click="node.toggle()"
            v-bind:class="handleClass">
      {{ treeHandleIcon }}
    </v-icon>
    <div @click.stop="clickNode()"
         @dblclick.stop="dblClickNode()"
         class="hp-tree-node-content"
         v-bind:class="contentClass">
      <slot name="node-content" :data="node.data"></slot>
    </div>

    <template v-if="!node.isLeaf() && node.expanded">
      <ul>
        <tree-node v-for="childNode in node.children"
                   :key="childNode.id"
                   :node="childNode"
                   @node-select="selectNode">
          <template slot-scope="{ data }" slot="node-content">
            <slot name="node-content" :data="data"></slot>
          </template>
        </tree-node>
      </ul>
    </template>
  </li>
</template>

<script>
  import _ from 'lodash';

  export default {
    name: 'tree-node',
    props: ['node'],
    data: function () {
      return {}
    },
    methods: {
      selectNode: function (node) {
        this.$emit('node-select', node)
      },

      dblClickNode: function () {
        this.clickNode.cancel();
        this.node.toggle();
      }
    },
    computed: {
      treeHandleIcon: function () {
        return this.node.isLeaf() ? 'fa-square-o'
            : (this.node.expanded ? 'fa-minus-square-o' : 'fa-plus-square-o')
      },

      contentClass: function () {
        return {
          selected: this.node.selected
        }
      },

      handleClass: function () {
        return {
          clickable: !this.node.isLeaf()
        }
      }
    },
    created: function () {
      this.clickNode = _.debounce(() => this.selectNode(this.node), 400);
    }
  }
</script>

<style lang="scss">
  @import "~compass-mixins/lib/compass";

  .hp-tree-node {
    .hp-tree-handle {
      &.clickable {
        cursor: pointer;
      }
      &.icon {
        font-size: 1em;
      }
    }

    .hp-tree-node-content {
      display: inline-block;
      background-color: transparent;
      padding: 0 0.2em;
      position: relative;

      @include transition(background-color 0.1s ease-in-out);

      &.selected {
        background-color: rgba(171, 169, 169, 0.25);
      }
    }
  }

</style>
