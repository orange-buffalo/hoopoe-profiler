import Vue from 'vue'

import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
Vue.use(Vuetify);

import injector from 'vue-inject'
Vue.use(injector);

require('./stylus/main.styl');
require('file-loader?name=[name].[ext]!../index.html');
require('./services/services');

require('vue2-animate/dist/vue2-animate.min.css');

import App from './app.vue'

new Vue({
  el: '#app',
  render: h => h(App)
});

