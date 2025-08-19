export type RunStatus = "idle" | "requestedRun" | "compiling" | "running"

export type State = {
    runStatus: RunStatus,
    output: string | null,
    error: string | null,
}

