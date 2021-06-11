import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, FormControlLabel, makeStyles, Switch, FormGroup, Checkbox, Slider} from "@material-ui/core";
import "@material-ui/styles"

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: false,
    })
    const [sliderValue, setSliderValue] = useState<number[]>([20, 37]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({ ...state, [event.target.name]: event.target.checked });
    };

    const handleSliderChange = (event: any, newValue: number | number[]) => {
        setSliderValue(newValue as number[]);
    };

    const css= makeStyles({
        optionPaneButton:{
            backgroundColor: "white",
            position: "fixed",
            bottom: "10px",
            left: "50%",
        },
        slider:{
            width: "400px",
        }
    });
    const style = css();


  return (
      <div>
          <WorldMap/>
          <Button className={style.optionPaneButton} onClick={() => setShowOptionPane(!showOptionPane)}>toggle overlay</Button>
          <ReactSlidingPane width={"100%"} isOpen={showOptionPane} onRequestClose={() => setShowOptionPane(false)} from={"bottom"} title={"Optionen"}>
              <FormGroup >
                  <FormControlLabel control={<Switch checked={button} onChange={() => setButton(!button)} />} label={"Test Button"} />
                  <FormControlLabel control={<Checkbox checked={state.checkBox} onChange={handleChange} name={"checkBox"}/>} label={"test Checkbox"} />
                  <FormControlLabel control={<Checkbox checked={state.checkBox2} onChange={handleChange} name={"checkBox2"}/>} label={"test Checkbox2"} />
                  <FormControlLabel control={
                      <Slider
                          value={sliderValue}
                          onChange={handleSliderChange}
                          valueLabelDisplay={"auto"}
                          aria-labelledby={"range-slider"}
                          name={"slider"}
                      />
                  } label={"test Slider"} className={style.slider}/>
              </FormGroup>
          </ReactSlidingPane>

      </div>
  );
}

export default App;
