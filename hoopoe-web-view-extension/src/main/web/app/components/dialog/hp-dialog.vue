<template>
  <v-dialog v-model="dialog.visible"
            fullscreen
            transition="dialog-bottom-transition"
            @keydown="dialog.$onKeyDown($event)">
    <slot></slot>
  </v-dialog>
</template>

<script>
  export default {
    name: 'hp-dialog',
    props: ['dialog']
  }
</script>