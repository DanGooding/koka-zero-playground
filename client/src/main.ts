import './style.css'
import type {State} from "./state.ts";
import {runButton, updateViewForState} from "./view.ts";

let state: State = {
    runStatus: "idle",
    output: "",
    error: ""
}

function runCode() {
    state.runStatus = "requestedRun"
    state.output = null
    state.error = null
    updateViewForState(state)

    setTimeout(() => {
        state.runStatus = "compiling"
        updateViewForState(state)
        setTimeout(() => {
            state.runStatus = "running"
            updateViewForState(state)
            setTimeout(() => {
                state.error = "segfault :("
                state.output = "hello\nworld\n:)"
                state.runStatus = "idle"
                updateViewForState(state)
            }, 300)
        }, 300)
    }, 300)
}

runButton.addEventListener('click', runCode)

