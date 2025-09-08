export type RequestOutcome = 'ok' | 'client error' | 'server error'
export type RequestDetails = {
    outcome: RequestOutcome
    toOpenSeconds?: number,
    toFirstResponseSeconds?: number,
    toCloseSeconds?: number,
}