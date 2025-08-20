import type {State} from "./state.ts";
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'

const runStatusDiv = document.querySelector<HTMLDivElement>('#run-status')!
const errorDiv = document.querySelector<HTMLDivElement>('#error')!
const outputDiv = document.querySelector<HTMLDivElement>('#output')!

export const runButton = document.querySelector<HTMLButtonElement>('#run-code')!
export const stdinInput = document.querySelector<HTMLInputElement>("#stdin")!
export const sourceCode = document.querySelector<HTMLTextAreaElement>('#source-code')!

sourceCode.value = fibonacciGeneratorCode

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
}

export function updateViewForState(state: State) {
    updateViewForRunStatus(state)
    outputDiv.textContent = state.output || ""
    errorDiv.textContent = state.error || ""
}
