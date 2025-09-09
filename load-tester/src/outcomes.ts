export type RequestOutcome = 'ok' | 'client error' | 'server error'
export type RequestEvent = 'opened' | 'closed'
export type RequestDetails = {
    outcome: RequestOutcome,
    toEventSeconds: Map<RequestEvent | string, number>
}