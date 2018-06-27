import * as React from 'react';
import {Constants} from 'model';
import {Graph} from '@greycat/greycat';
import './App.css';
import IndexViewer from './IndexViewer';

const logo = require('./logo.svg');


class App extends React.Component<{ graph: Graph }, {}> {
    render() {
        return (
            <div className="App">
                <div className="App-header">
                    <h2>{Constants.POWER}</h2>
                    <img src={logo} className="App-logo" alt="logo" />
                </div>
                <IndexViewer graph={this.props.graph} indexName="sensors" />
            </div>
        );
    }



}

export default App;
