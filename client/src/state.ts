export type RunStatus = "idle" | "connecting" | "requestedRun" | "compiling" | "startingRun" | "running"
export type CodeExample = "fibonacciGenerator" | "pythagoreanSearch" | "blank"

export type State = {
    codeExampleState: {
        base: CodeExample,
        needsLoad: boolean,
    },
    runStatus: RunStatus,
    websocket: WebSocket | null,
    output: ['input' | 'output', string][],
    error: string | null,
}

export function manageState(initial: State, onChange: (state: Readonly<State>) => void): {
    setState: (newState: Partial<State>) => void,
    modifyState: (modify: (state: State) => void) => void,
    getState: () => Readonly<State>
} {
    let state = initial
    onChange(state)

    const setState = (delta: Partial<State>) => {
        state = {...state, ...delta}
        onChange(state)
    }

    const modifyState = (update: (state: State) => void) => {
        update(state)
        onChange(state)
    }

    const getState = () => state

    return {setState, modifyState, getState}
}
