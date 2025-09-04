import type { State } from "./state.ts";
import fibonacciGeneratorCode from './fibonacci-generator.kk?raw'
import escape from 'escape-html'
import { kokaGrammar, kokaLanguage } from './syntax'
import { basicEditor } from 'prism-code-editor/setups'
import { languages } from "prism-code-editor/prism"
import { languageMap } from "prism-code-editor"

const runStatusDiv = document.querySelector<HTMLDivElement>('#run-status')!
const errorDiv = document.querySelector<HTMLDivElement>('#error')!
const inputOutputPre = document.querySelector<HTMLPreElement>('#input-output')!

export const runButton = document.querySelector<HTMLButtonElement>('#run-code')!
export const stdinInput = document.querySelector<HTMLInputElement>("#stdin")!

languages['koka'] = kokaGrammar
languageMap['koka'] = kokaLanguage

export const editor = basicEditor(
    '#editor',
    {
        language: 'koka',
        theme: 'prism',
        value: fibonacciGeneratorCode
    })

function statusDescription(state: State): string {
    switch (state.runStatus) {
        case "idle":
            if (state.error != null) {
                return "failed"
            } else {
                return ""
            }
        case "connecting":
            return "connecting..."
        case "requestedRun":
            return "awaiting run..."
        case "compiling":
            return "compiling..."
        case "startingRun":
            return "starting..."
        case "running":
            return "running..."
    }
}

function updateViewForRunStatus(state: State) {
    runStatusDiv.textContent = statusDescription(state);

    runButton.disabled = state.runStatus !== "idle"
    stdinInput.disabled = state.runStatus !== "running"

    // Hide placeholder when disabled
    if (stdinInput.disabled) {
        stdinInput.placeholder = "";
    } else {
        stdinInput.placeholder = "input";
    }
}

function updateTerminalForState(state: State) {
    errorDiv.textContent = state.error || ""

    // Save stdin value and focus state
    const stdinHadFocus = document.activeElement === stdinInput;
    const stdinValue = stdinInput.value;

    // Render output HTML (excluding stdin input)
    inputOutputPre.innerHTML =
        state.output.map(([kind, content]) =>
            `<span class="terminal-${kind}">${escape(content)}</span>`
        ).join('');

    // Append the stdin input at the end
    inputOutputPre.appendChild(stdinInput);

    // Restore value and focus if needed
    stdinInput.value = stdinValue;
    if (stdinHadFocus) stdinInput.focus();
}

export function updateViewForState(state: State) {
    updateViewForRunStatus(state)
    updateTerminalForState(state)
}
