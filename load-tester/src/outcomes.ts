export type RequestOutcome = 'ok' | 'client error' | 'server error'
export type RequestDetails = {
    outcome: RequestOutcome
    toOpenMs?: number,
    toFirstResponseMs?: number,
    toCloseMs?: number,
}