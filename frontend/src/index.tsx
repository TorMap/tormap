import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import "react-sliding-pane/dist/react-sliding-pane.css";
import {App} from "./components/app";

ReactDOM.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
  document.getElementById('root')
);
