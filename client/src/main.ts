import './style.css'
import {manageState} from "./state.ts";
import {runButton, sourceCode, updateViewForState} from "./view.ts";

const {setState, modifyState, getRunStatus} = manageState(
    {
        runStatus: "idle",
        output: "",
        error: null
    },
    updateViewForState
)

function runCode() {
    if (getRunStatus() !== "idle") {
        return
    }

    setState({
        runStatus: "requestedRun",
        output: "",
        error: null,
    })

    const websocket = new WebSocket(import.meta.env.VITE_WS_URL_COMPILE_AND_RUN)
    setState({runStatus: "connecting"})

    websocket.onopen = (event) => {
        console.log("Connection opened", event)

        websocket.send(JSON.stringify({
            "compile-and-run": {
                sourceCode: {
                    code: sourceCode.value
                }
            }
        }))

        setState({runStatus: "requestedRun"})
    }

    websocket.onmessage = (e: MessageEvent) => {
        const message = JSON.parse(e.data)
        if (message.hasOwnProperty("another-request-in-progress")) {
            setState({
                runStatus: "idle",
                error: "cannot run - another run in progress",
            })

        } else if (message.hasOwnProperty("starting-compilation")) {
            setState({runStatus: "compiling"})

        } else if (message.hasOwnProperty("running")) {
            setState({runStatus: "running"})

        } else if (message.hasOwnProperty("stdout")) {
            modifyState((state) => {
                state.output += message.stdout.content
            })

        } else if (message.hasOwnProperty("error")) {
            setState({
                runStatus: "idle",
                error: message.error.message
            })

        } else if (message.hasOwnProperty("done")) {
            setState({runStatus: "idle"})

        } else if (message.hasOwnProperty("interrupted")) {
            setState({
                runStatus: "idle",
                error: `interrupted: ${message.interrupted.message}`
            })
        }
    }
    websocket.onerror = () => {
        setState({
            runStatus: "idle",
            error: `websocket error`
        })
    }
    websocket.onclose = (e: CloseEvent) => {
        if (!e.wasClean) {
            setState({
                runStatus: "idle",
                error: `websocket closed with error`
            })
        }
    }

}

runButton.addEventListener('click', runCode)

