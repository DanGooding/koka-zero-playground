import './style.css'
import {manageState} from "./state.ts";
import {runButton, createEditor, stdinInput, codeExampleButtons, updateViewForState} from "./view.ts";

const editor = createEditor(() => {
    // populate editor with code example once it loads
    modifyState(state => state.codeExampleState.needsLoad = true);
})

const {setState, modifyState, getState} = manageState(
    {
        codeExampleState: {
            base: "fibonacciGenerator",
            needsLoad: true,
        },
        runStatus: "idle",
        output: [],
        error: null,
        websocket: null,
    },
    (state) => updateViewForState(state, editor)
)

function runCode() {
    if (getState().runStatus !== "idle") {
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
            "@type": "compile-and-run",
            code: editor.value
        }))

        setState({runStatus: "requestedRun"})
    }

    websocket.onmessage = (e: MessageEvent) => {
        if (e.target !== getState().websocket) return;

        const message = JSON.parse(e.data)
        switch (message["@type"]) {
            case "another-request-in-progress":
                setState({
                    runStatus: "idle",
                    error: "cannot run - another run in progress",
                })
                break

            case "starting-compilation":
                setState({runStatus: "compiling"})
                break


            case "starting-run":
                setState({runStatus: "startingRun"})
                break

            case "running":
                setState({runStatus: "running"})
                break

            case "stdout":
                modifyState((state) => {
                    state.output.push(['output', message.content])
                })
                break

            case "error":
                setState({
                    runStatus: "idle",
                    error: message.message,
                    websocket: null
                })
                break

            case "done":
                setState({runStatus: "idle", websocket: null})
                break

            case "interrupted":
                setState({
                    runStatus: "idle",
                    error: `interrupted: ${message.message}`,
                    websocket: null
                })
                break

            default:
                console.error("unexpected message type", message)
        }


    }
    websocket.onerror = (e: Event) => {
        if (e.target !== getState().websocket) return;

        setState({
            runStatus: "idle",
            error: "websocket error",
            websocket: null
        })
    }
    websocket.onclose = (e: CloseEvent) => {
        if (e.target !== getState().websocket) return;

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
    if (getState().runStatus === "idle") return
    const websocket = getState().websocket
    if (!websocket) return

    const content = stdinInput.value + '\n'
    if (/^\s*$/.test(content)) return;

    websocket.send(JSON.stringify({
        "@type": "stdin",
        content
    }))
    modifyState(state => {
        state.output.push(['input', content])
    })
    stdinInput.value = ""
}

runButton.addEventListener('click', runCode)
stdinInput.addEventListener('keyup', (event) => {
    if (event.key === 'Enter') sendStdin()
})
for (const [codeExample, button] of codeExampleButtons.entries()) {
    button.addEventListener('click', () => {
        modifyState(state => {
            state.codeExampleState.base = codeExample
            state.codeExampleState.needsLoad = true
        })
    })
}