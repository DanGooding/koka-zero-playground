export type RequestOutcome = 'ok' | 'client error' | 'server error' | 'transport error'
export type RequestEvent = 'opened' | 'closed'
export type RequestDetails = {
    outcome: RequestOutcome,
    toEventSeconds: Map<RequestEvent | string, number>
}
export type RequestId = number
export type RequestState = string