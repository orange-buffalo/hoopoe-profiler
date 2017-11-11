<template>
  <li class="hp-tree-item">
    <v-icon class="hp-tree-handle" @click="node.toggle()">
      {{ treeHandleIcon }}
    </v-icon>
    <slot name="node-content" :data="node.data"></slot>

    <template v-if="!node.isLeaf() && node.expanded">
      <ul>
        <tree-item v-for="childNode in node.children"
                   :key="childNode.id"
                   :node="childNode">
          <template slot-scope="{ data }" slot="node-content">
            <slot name="node-content" :data="data"></slot>
          </template>
        </tree-item>
      </ul>
    </template>
  </li>
</template>

<script>
  export default {
    name: 'tree-item',
    props: ['node'],
    data: function () {
      return {}
    },
    methods: {
      invokeAction: function () {
        this.$emit('action-invoked')
      }
    },
    computed: {
      treeHandleIcon: function () {
        return this.node.isLeaf() ? 'fa-square-o'
            : (this.node.expanded ? 'fa-minus-square-o' : 'fa-plus-square-o')
      }
    },
    created: function () {
    }
  }
</script>

<style lang="scss">

  .hp-tree-item {
    .hp-tree-handle {
      cursor: pointer;
      &.icon {
        font-size: 1em;
      }
    }
  }

</style>
