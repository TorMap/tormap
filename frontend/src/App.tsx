import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button} from "@material-ui/core";
import "@material-ui/styles"
import "./app.css"

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
 //ToDo: Button and optionPane styling
  return (
      <div>
          <WorldMap/>
          <Button onClick={() => setShowOptionPane(!showOptionPane)} >toggle overlay</Button>
          <ReactSlidingPane isOpen={showOptionPane} onRequestClose={() => setShowOptionPane(false)} from={"bottom"} title={"Optionen"}>
              <h1>optionen</h1>
          </ReactSlidingPane>

      </div>
  );
}

export default App;
