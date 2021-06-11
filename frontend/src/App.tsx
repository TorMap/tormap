import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, makeStyles} from "@material-ui/core";
import "@material-ui/styles"
import "./app.css"

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
 //ToDo: Button and optionPane styling
    const css= makeStyles({
        root:{
            backgroundColor: "white",
            position: "fixed",
            bottom: "10px",
            left: "50%",
        }
    });
    const style = css();


  return (
      <div>
          <WorldMap/>
          <Button className={style.root} onClick={() => setShowOptionPane(!showOptionPane)}>toggle overlay</Button>
          <ReactSlidingPane width={"100%"} isOpen={showOptionPane} onRequestClose={() => setShowOptionPane(false)} from={"bottom"} title={"Optionen"}>
              <h1>optionen</h1>
          </ReactSlidingPane>

      </div>
  );
}

export default App;
