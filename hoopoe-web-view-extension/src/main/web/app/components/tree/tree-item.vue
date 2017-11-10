<template>
  <li class="hp-tree-item">
    <v-icon class="hp-tree-handle"
            @click="node.toggle()">
      {{ node.expanded ? 'fa-minus-square-o' : 'fa-plus-square-o' }}
    </v-icon>
    <slot name="node-content" :data="node.data"></slot>

    <template v-if="!node.isLeaf() && node.expanded">
      <ul>
        <tree-item v-for="childNode in node.children"
                   :key="childNode.id"
                   :node="childNode">
          <template slot-scope="props" slot="node-content">
            <slot name="node-content" :data="props.data"></slot>
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
    computed: {},
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
