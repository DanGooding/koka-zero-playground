import type {State} from "./state.ts";
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'
import escape from 'escape-html'
import { basicEditor } from 'prism-code-editor/setups'

const runStatusDiv = document.querySelector<HTMLDivElement>('#run-status')!
const errorDiv = document.querySelector<HTMLDivElement>('#error')!
const inputOutputPre = document.querySelector<HTMLPreElement>('#input-output')!

export const runButton = document.querySelector<HTMLButtonElement>('#run-code')!
export const stdinInput = document.querySelector<HTMLInputElement>("#stdin")!

export const editor = basicEditor(
    '#editor',
    {
        language: 'html',
        theme: 'prism',
        value: fibonacciGeneratorCode
    })

function updateViewForRunStatus(state: State) {
    let content: string;
    switch (state.runStatus) {
        case "idle":
            if (state.error != null) {
                content = "failed"
            } else {
                content = ""
            }
            break;
        case "connecting":
            content = "connecting..."
            break
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

    runButton.disabled = state.runStatus !== "idle"
    stdinInput.disabled = state.runStatus === "idle"
}

function updateTerminalForState(state: State) {
    errorDiv.textContent = state.error || ""

    inputOutputPre.innerHTML =
        state.output.map(([kind, content]) =>
            `<span class="terminal-${kind}">${escape(content)}</span>`
        ).join('')
}

export function updateViewForState(state: State) {
    updateViewForRunStatus(state)
    updateTerminalForState(state)
}
