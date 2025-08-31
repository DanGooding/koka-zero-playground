import './style.css'
import {manageState} from "./state.ts";
import {runButton, editor, stdinInput, updateViewForState} from "./view.ts";

const {setState, modifyState, getRunStatus, getWebSocket} = manageState(
    {
        runStatus: "idle",
        output: [],
        error: null,
        websocket: null,
    },
    updateViewForState
)

function runCode() {
    if (getRunStatus() !== "idle") {
        return
    }

    setState({
        runStatus: "requestedRun",
        output: [],
        error: null,
        websocket: null
    })

    const websocket = new WebSocket(import.meta.env.VITE_WS_URL_COMPILE_AND_RUN)
    setState({runStatus: "connecting", websocket})

    websocket.onopen = (event) => {
        console.log("Connection opened", event)

        websocket.send(JSON.stringify({
            "compile-and-run": {
                sourceCode: {
                    code: editor.value
                }
            }
        }))

        setState({runStatus: "requestedRun"})
    }

    websocket.onmessage = (e: MessageEvent) => {
        if (e.target !== getWebSocket()) return;

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
                state.output.push(['output', message.stdout.content])
            })

        } else if (message.hasOwnProperty("error")) {
            setState({
                runStatus: "idle",
                error: message.error.message,
                websocket: null
            })

        } else if (message.hasOwnProperty("done")) {
            setState({runStatus: "idle", websocket: null})

        } else if (message.hasOwnProperty("interrupted")) {
            setState({
                runStatus: "idle",
                error: `interrupted: ${message.interrupted.message}`,
                websocket: null
            })
        }
    }
    websocket.onerror = (e: Event) => {
        if (e.target !== getWebSocket()) return;

        setState({
            runStatus: "idle",
            error: "websocket error",
            websocket: null
        })
    }
    websocket.onclose = (e: CloseEvent) => {
        if (e.target !== getWebSocket()) return;

        let error = null

        // NORMAL or GOING_AWAY are successful closes, otherwise it's an error
        if (e.code !== 1000 && e.code !== 1001) {
            error = `websocket closed with error:\n ${e.reason}`
        }
        setState({
            runStatus: "idle",
            websocket: null,
            error
        })
    }

}

function sendStdin() {
    if (getRunStatus() === "idle") return
    const websocket = getWebSocket()
    if (!websocket) return

    const content = stdinInput.value + '\n'
    if (!content) return

    websocket.send(JSON.stringify({
        stdin: {
            content
        }
    }))
    modifyState(state => {
        // TODO: distinguish input from output
        state.output.push(['input', content])
    })
    stdinInput.value = ""
}

runButton.addEventListener('click', runCode)
stdinInput.addEventListener('keyup', (event) => {
    if (event.key === 'Enter') sendStdin()
})
