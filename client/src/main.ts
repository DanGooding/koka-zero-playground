import './style.css'
import type {State} from "./state.ts";
import {runButton, sourceCode, updateViewForState} from "./view.ts";

let state: State = {
    runStatus: "idle",
    output: "",
    error: null
}
updateViewForState(state)

function runCode() {
    if (state.runStatus !== "idle") {
        return
    }

    state.runStatus = "requestedRun"
    state.output = ""
    state.error = null
    updateViewForState(state)

    // TODO: get url from env
    const websocket = new WebSocket("ws://localhost:5173/ws/compile-and-run")
    state.runStatus = "connecting"
    updateViewForState(state)

    // TODO: on error (e.g. 403 bad origin) - actually detect this

    websocket.onopen = () => {
        websocket.send(JSON.stringify({
            "compile-and-run": {
                sourceCode: {
                    code: sourceCode.value
                }
            }
        }))

        state.runStatus = "requestedRun"
        updateViewForState(state)
    }

    websocket.onmessage = (e: MessageEvent) => {
        const message = JSON.parse(e.data)
        if (message.hasOwnProperty("another-request-in-progress")) {
            state.runStatus = "idle"
            state.error = "run failed - another run in progress"

        } else if (message.hasOwnProperty("starting-compilation")) {
            state.runStatus = "compiling"

        } else if (message.hasOwnProperty("running")) {
            state.runStatus = "running"

        } else if (message.hasOwnProperty("stdout")) {
            state.output += message.stdout.content

        } else if (message.hasOwnProperty("error")) {
            state.error = message.error.message
            state.runStatus = "idle"

        } else if (message.hasOwnProperty("done")) {
            state.runStatus = "idle"

        } else if (message.hasOwnProperty("interrupted")) {
            state.runStatus = "idle"
            state.error = `interrupted: ${message.interrupted.message}`
        }

        updateViewForState(state)
    }
    websocket.onerror = (e: Event) => {
        state.runStatus = "idle"
        state.error = `websocket error: ${e}`
    }
    websocket.onclose = (e: CloseEvent) => {
        if (!e.wasClean) {
            state.error = `websocket closed with error: ${e}`
        }
    }

}

runButton.addEventListener('click', runCode)

