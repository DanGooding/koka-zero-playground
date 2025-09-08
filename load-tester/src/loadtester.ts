import type { RequestDetails, RequestOutcome } from './outcomes.js'

type Requestor = (onComplete: (summary: RequestDetails) => void) => void

export default class LoadTester {
    readonly requestsPerSecond: number
    readonly sendRequest: Requestor

    totalCount = 0
    outcomeCounts = new Map<RequestOutcome, number>()

    constructor(requestsPerSecond: number, sendRequest: Requestor) {
        this.requestsPerSecond = requestsPerSecond
        this.sendRequest = sendRequest
    }

    printSummary() {
        const outcomeFractions: [RequestOutcome, string][] =
            Array.from(this.outcomeCounts.entries())
                .map(
                    ([outcome, count]: [RequestOutcome, number]) => {
                        return [outcome, (count / this.totalCount * 100).toFixed(1)];
                    })

        console.log(new Map<RequestOutcome, string>(outcomeFractions))
    }

    run(): void {
        const onComplete = (summary: RequestDetails) => {
            this.outcomeCounts.set(summary.outcome,
                (this.outcomeCounts.get(summary.outcome) ?? 0) + 1);
            this.totalCount += 1
        }

        setInterval(() => {
            this.sendRequest(onComplete)
        }, 1000 / this.requestsPerSecond)

        const updatesPerSecond = 5
        setInterval(() => {
            this.printSummary()
        }, 1000 / updatesPerSecond)
    }
}
