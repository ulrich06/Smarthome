import * as React from 'react';
import * as ReactDOM from 'react-dom';
import App from './App';

import registerServiceWorker from './registerServiceWorker';
import './index.css';
import {Graph, GraphBuilder} from '@greycat/greycat';
import {WSClient} from '@greycat/greycat-websocket';
import {ModelPlugin} from 'model';

let graph: Graph = GraphBuilder.newBuilder()
    .withPlugin(new ModelPlugin())
    .withStorage(new WSClient('ws://127.0.0.1:8081/ws'))
    .build();
graph.connect(function (isConnected: boolean) {
    ReactDOM.render(<App graph={graph} />, document.getElementById('root') as HTMLElement);
    registerServiceWorker();
});
