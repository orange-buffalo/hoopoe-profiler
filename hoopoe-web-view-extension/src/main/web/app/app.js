require('file-loader?name=[name].[ext]!../index.html');
import Vue from 'vue'
import App from './App.vue'

new Vue({
  el: '#app',
  render: h => h(App)
});

