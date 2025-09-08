import type { RequestDetails, RequestOutcome } from './outcomes.js'

type Requestor = (onComplete: (summary: RequestDetails) => void) => void

class BucketSummary {
    totalCount = 0
    outcomeCounts = new Map<RequestOutcome, number>()

    addSummary(summary: RequestDetails) {
        this.outcomeCounts.set(summary.outcome,
            (this.outcomeCounts.get(summary.outcome) ?? 0) + 1);
        this.totalCount += 1
    }

    getFractions(): Map<RequestOutcome, string> {
        const outcomeFractions = new Map<RequestOutcome, string>
        for (const [outcome, count] of this.outcomeCounts.entries()) {
            outcomeFractions.set(outcome, ((count / this.totalCount) * 100).toFixed(1))
        }
        return  outcomeFractions
    }

    static combine(buckets: BucketSummary[]) {
        const totalBucket = new BucketSummary()
        for (const bucket of buckets) {
            totalBucket.totalCount += bucket.totalCount
            for (const [outcome, count] of bucket.outcomeCounts) {
                totalBucket.outcomeCounts.set(outcome,
                    (totalBucket.outcomeCounts.get(outcome) ?? 0) + count)
            }
        }
        return totalBucket
    }
}

export default class LoadTester {
    readonly requestsPerSecond: number
    readonly sendRequest: Requestor

    readonly windowSeconds: number
    readonly bucketsPerWindow: number

    // the last bucket is currently being populated
    // its time window has not yet fully passed
    // so it shouldn't be used in calculations
    buckets: BucketSummary[]

    constructor(requestsPerSecond: number, sendRequest: Requestor, windowSeconds: number = 10) {
        this.requestsPerSecond = requestsPerSecond
        this.sendRequest = sendRequest

        this.bucketsPerWindow = 10
        this.windowSeconds = windowSeconds

        this.buckets = [new BucketSummary()]
    }

    printSummary() {

        // ignore the initial incomplete bucket
        const allWindowBuckets = this.buckets.slice(0, this.buckets.length - 1)
        const windowBucket = BucketSummary.combine(allWindowBuckets)
        const windowSeconds = allWindowBuckets.length / this.bucketsPerWindow * this.windowSeconds

        const outcomeFractions = windowBucket.getFractions()
        const effectiveRPS = (windowBucket.outcomeCounts.get('ok') ?? 0) / windowSeconds

        console.log({
            outcomeFractions,
            effectiveRPS,
        })
    }

    run(): void {
        const onComplete = (details: RequestDetails) => {
            this.buckets[this.buckets.length - 1].addSummary(details)
        }

        // window shift loop
        setInterval(() => {
            this.buckets.push(new BucketSummary())
            if (this.buckets.length > this.bucketsPerWindow + 1) {
                this.buckets.shift()
            }

            this.printSummary()

        }, 1000 * this.windowSeconds / this.bucketsPerWindow)

        // request loop
        setInterval(() => {
            this.sendRequest(onComplete)
        }, 1000 / this.requestsPerSecond)
    }
}
