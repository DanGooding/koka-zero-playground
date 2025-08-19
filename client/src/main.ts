import './style.css'
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'

const sourceCode = document.querySelector<HTMLTextAreaElement>('#source-code')!
sourceCode.textContent = fibonacciGeneratorCode

const runButton = document.querySelector<HTMLButtonElement>('#run-code')!

// TODO: these are essentially reactive
const runStatusDiv = document.querySelector<HTMLDivElement>('#run-status')!
const errorDiv = document.querySelector<HTMLDivElement>('#error')!
const outputDiv = document.querySelector<HTMLDivElement>('#output')!

enum RunStatus {
    Idle,
    RequestedRun,
    Compiling,
    Running,
}

type State = {
    runStatus: RunStatus,
    output: string | null,
    error: string | null,
}


function updateViewForRunStatus(status: RunStatus) {
    var content: string;
    switch (status) {
        case RunStatus.Idle:
            content = ""
            break;
        case RunStatus.RequestedRun:
            content = "awaiting run..."
            break
        case RunStatus.Compiling:
            content = "compiling..."
            break
        case RunStatus.Running:
            content = "running..."
            break
    }
    runStatusDiv.textContent = content
}

function updateViewForState(state: State) {
    updateViewForRunStatus(state.runStatus)
    outputDiv.textContent = state.output || ""
    errorDiv.textContent = state.error || ""
}

var state: State = {
    runStatus: RunStatus.Idle,
    output: "",
    error: ""
}

function runCode() {
    state.runStatus = RunStatus.RequestedRun
    state.output = null
    state.error = null
    updateViewForState(state)

    setTimeout(() => {
        state.runStatus = RunStatus.Compiling
        updateViewForState(state)
        setTimeout(() => {
            state.runStatus = RunStatus.Running
            updateViewForState(state)
            setTimeout(() => {
                state.error = "segfault :("
                state.output = "hello\nworld\n:)"
                state.runStatus = RunStatus.Idle
                updateViewForState(state)
            }, 300)
        }, 300)
    }, 300)
}

runButton.addEventListener('click', runCode)

