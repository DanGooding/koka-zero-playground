import type { RequestDetails, RequestEvent, RequestOutcome } from './outcomes.js'
import stats from 'stats-lite'

type Requestor = (onComplete: (summary: RequestDetails) => void) => void

class BucketSummary {
    totalCount = 0
    outcomeCounts = new Map<RequestOutcome, number>()
    eachOkEventLatencySeconds: Map<RequestEvent, Array<number>> = new Map<RequestEvent, Array<number>>()

    addSummary(summary: RequestDetails) {
        this.outcomeCounts.set(summary.outcome,
            (this.outcomeCounts.get(summary.outcome) ?? 0) + 1);
        this.totalCount += 1

        if (summary.outcome === 'ok') {
            for (const [event, latency] of summary.toEventSeconds) {
                const latencies = this.eachOkEventLatencySeconds.get(event)
                if (latencies == null) {
                    this.eachOkEventLatencySeconds.set(event, [latency])
                }else {
                    latencies.push(latency)
                }
            }
        }
    }

    getPercentages(): Map<RequestOutcome, string> {
        const outcomePercentages = new Map<RequestOutcome, string>
        for (const [outcome, count] of this.outcomeCounts.entries()) {
            outcomePercentages.set(outcome, ((count / this.totalCount) * 100).toFixed(1))
        }
        return outcomePercentages
    }

    static combine(buckets: BucketSummary[]) {
        const totalBucket = new BucketSummary()
        for (const bucket of buckets) {
            totalBucket.totalCount += bucket.totalCount
            for (const [outcome, count] of bucket.outcomeCounts) {
                totalBucket.outcomeCounts.set(outcome,
                    (totalBucket.outcomeCounts.get(outcome) ?? 0) + count)
            }
            for (const [event, latencies] of bucket.eachOkEventLatencySeconds) {
                const totalBucketLatencies = totalBucket.eachOkEventLatencySeconds.get(event)
                if (totalBucketLatencies == null) {
                    totalBucket.eachOkEventLatencySeconds.set(event, Array.from(latencies))
                }else {
                    totalBucket.eachOkEventLatencySeconds.set(event, totalBucketLatencies.concat(latencies))
                }
            }
        }
        return totalBucket
    }
}

export default class LoadTester {
    readonly requestsPerSecond: number
    readonly sendRequest: Requestor

    readonly windowSeconds: number = 10
    readonly bucketsPerWindow: number = 10

    readonly printRawStats: boolean

    // the last bucket is currently being populated
    // its time window has not yet fully passed
    // so it shouldn't be used in calculations
    buckets: BucketSummary[]

    constructor(requestsPerSecond: number, sendRequest: Requestor, printRawStats: boolean = false) {
        this.requestsPerSecond = requestsPerSecond
        this.sendRequest = sendRequest
        this.printRawStats = printRawStats

        this.buckets = [new BucketSummary()]
    }

    printSummary() {

        // ignore the initial incomplete bucket
        const allWindowBuckets = this.buckets.slice(0, this.buckets.length - 1)
        const windowBucket = BucketSummary.combine(allWindowBuckets)
        const windowSeconds = allWindowBuckets.length / this.bucketsPerWindow * this.windowSeconds

        if (this.printRawStats) {
            console.log(windowBucket)
        }

        const outcomePercentages = windowBucket.getPercentages()
        const effectiveRPS = (windowBucket.outcomeCounts.get('ok') ?? 0) / windowSeconds

        const totalOkLatencySeconds = stats.sum(windowBucket.eachOkEventLatencySeconds.get('closed') ?? [])
        const concurrency = totalOkLatencySeconds / windowSeconds

        const medianOkLatencyByEvent =
            new Map<RequestEvent, string>(
            Array.from(windowBucket.eachOkEventLatencySeconds)
                .map(([event, latencies]) =>
                    [event, stats.median(latencies).toFixed(3)]))

        console.log({
            outcomePercentages,
            effectiveRPS: effectiveRPS.toFixed(0),
            concurrency: concurrency.toFixed(1),
            medianOkLatencyByEvent,
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
