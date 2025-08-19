export type RunStatus = "idle" | "connecting" | "requestedRun" | "compiling" | "running"

export type State = {
    runStatus: RunStatus,
    output: string,
    error: string | null,
}

