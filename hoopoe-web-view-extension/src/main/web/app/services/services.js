import injector from 'vue-inject'
import axios from 'axios'

injector.constant('$http', axios);

require('./json-rpc');
require('./event-bus');