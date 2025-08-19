import './style.css'
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'

const sourceCode = document.querySelector<HTMLTextAreaElement>('#source-code')!
sourceCode.textContent = fibonacciGeneratorCode

const runButton = document.querySelector<HTMLButtonElement>('#run-code')!

const runStatusDiv = document.querySelector<HTMLDivElement>('#run-status')!
const errorDiv = document.querySelector<HTMLDivElement>('#error')!
const outputDiv = document.querySelector<HTMLDivElement>('#output')!

type RunStatus = "idle" | "requestedRun" | "compiling" | "running"

type State = {
    runStatus: RunStatus,
    output: string | null,
    error: string | null,
}


function updateViewForRunStatus(state: State) {
    var content: string;
    switch (state.runStatus) {
        case "idle":
            if (state.error != null) {
                content = "failed"
            } else {
                content = ""
            }
            break;
        case "requestedRun":
            content = "awaiting run..."
            break
        case "compiling":
            content = "compiling..."
            break
        case "running":
            content = "running..."
            break
    }
    runStatusDiv.textContent = content
}

function updateViewForState(state: State) {
    updateViewForRunStatus(state)
    outputDiv.textContent = state.output || ""
    errorDiv.textContent = state.error || ""
}

var state: State = {
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

