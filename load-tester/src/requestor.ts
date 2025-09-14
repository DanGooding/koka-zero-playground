import type { RequestDetails, RequestId, RequestState } from './outcomes.js'

export function secondsSince(then: number): number {
    return (Date.now() - then) / 1000
}

export default abstract class Requestor {
    url: URL

    constructor(url: URL) {
        this.url = url
    }

    abstract request(
        id: RequestId,
        onStateChange: (id: RequestId, newState: RequestState) => void,
        onComplete: (id: RequestId, summary: RequestDetails) => void): RequestState
}
