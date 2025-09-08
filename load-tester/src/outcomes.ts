export type RequestOutcome = 'ok' | 'client error' | 'server error'
export type RequestEvent = 'opened' | 'first-response' | 'requested-run' | 'running' | 'closed'
export type RequestDetails = {
    outcome: RequestOutcome,
    toEventSeconds: Map<RequestEvent, number>
}