import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, FormControlLabel, makeStyles, Switch, FormGroup, Checkbox, Slider, Typography} from "@material-ui/core";
import "@material-ui/styles"

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: false,
    })
    const [sliderValue, setSliderValue] = useState<number[]>([1, monthsSinceBeginning()]);

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

    function monthsSinceBeginning(){
        const today = new Date();
        const currentYear = today.getFullYear();
        const currentMonth = today.getMonth();
        return (currentYear-2007)*12-10 + currentMonth;
    }

    function mapValueToDateString(value:number){
        value += 10;
        const month = value % 12;
        const year = (value - month) / 12;
        return (2007 + year) + "-" + (month + 1);
    }

  return (
      <div>
          <WorldMap dateRangeToDisplay={{startDate: new Date("2021-05-01"), endDate: new Date("2021-06-01")}}/>
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
                          min={1}
                          max={monthsSinceBeginning()}
                          getAriaValueText={mapValueToDateString}
                      />
                  } label={"test Slider"} className={style.slider}/>
                  <Typography>
                      {"Von " + mapValueToDateString(sliderValue[0]) + " bis " + mapValueToDateString(sliderValue[1])}
                  </Typography>
              </FormGroup>
          </ReactSlidingPane>

      </div>
  );
}

export default App;
