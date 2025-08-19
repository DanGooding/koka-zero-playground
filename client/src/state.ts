export type RunStatus = "idle" | "connecting" | "requestedRun" | "compiling" | "running"

export type State = {
    runStatus: RunStatus,
    output: string,
    error: string | null,
}

export function manageState(initial: State, onChange: (state: State) => void): {
    setState: (newState: Partial<State>) => void,
    modifyState: (modify: (state: State) => void) => void,
    // getters intended to help prevent accidentally setting the state object's fields directly
    getRunStatus: () => RunStatus,
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

    const getRunStatus = () => state.runStatus

    return {setState, modifyState, getRunStatus}
}
