import Vue from 'vue';
import injector from 'vue-inject'

class Events {
  constructor(){
    this.bus = new Vue();
  }

  fire(eventName, eventObj) {
    this.bus.$emit(eventName, eventObj)
  }

  subscribe(eventName, listener) {
    this.bus.$on(eventName, listener)
  }

}

injector.service('$events', Events);