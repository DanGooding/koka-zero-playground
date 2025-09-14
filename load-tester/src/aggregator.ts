import type { RequestDetails, RequestOutcome, RequestId, RequestState } from './outcomes.js'
import stats from 'stats-lite'

class BucketSummary {
    totalCount = 0
    outcomeCounts = new Map<RequestOutcome, number>()
    eachOkEventLatencySeconds: Map<string, Array<number>> = new Map<string, Array<number>>()

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

export default class Aggregator {
    readonly windowSeconds: number = 10
    readonly bucketsPerWindow: number = 10

    readonly printRawStats: boolean

    // the last bucket is currently being populated
    // its time window has not yet fully passed
    // so it shouldn't be used in calculations
    buckets: BucketSummary[]

    inFlightRequestStates: Map<RequestId, RequestState>

    constructor(printRawStats: boolean = false) {
        this.printRawStats = printRawStats

        this.buckets = [new BucketSummary()]
        this.inFlightRequestStates = new Map<RequestId, RequestState>()
    }

    getWindow(): { windowBucket: BucketSummary, windowSeconds: number } {
        // ignore the initial incomplete bucket
        const allWindowBuckets = this.buckets.slice(0, this.buckets.length - 1)
        const windowBucket = BucketSummary.combine(allWindowBuckets)
        const windowSeconds = allWindowBuckets.length / this.bucketsPerWindow * this.windowSeconds

        return { windowBucket, windowSeconds }
    }

    getSummary(): object {
        const {windowBucket, windowSeconds} = this.getWindow()

        const outcomePercentages = windowBucket.getPercentages()
        const effectiveRPS = (windowBucket.outcomeCounts.get('ok') ?? 0) / windowSeconds

        const totalOkLatencySeconds = stats.sum(windowBucket.eachOkEventLatencySeconds.get('closed') ?? [])
        const concurrency = totalOkLatencySeconds / windowSeconds

        const medianOkLatencyByEvent =
            new Map<string, string>(
                Array.from(windowBucket.eachOkEventLatencySeconds)
                    .map(([event, latencies]) =>
                        [event, stats.median(latencies).toFixed(3)]))

        const inFlightRequests = new Map<RequestState, number>();
        for (const state of this.inFlightRequestStates.values()) {
            inFlightRequests.set(state, (inFlightRequests.get(state) ?? 0) + 1)
        }

        const summary = {
            outcomePercentages,
            effectiveRPS: effectiveRPS.toFixed(0),
            concurrency: concurrency.toFixed(1),
            medianOkLatencyByEvent,
            inFlightRequests,
        }

        if (this.printRawStats) {
            return { summary, rawStats: windowBucket }
        }else {
            return summary
        }
    }

    onRequestComplete = (id: RequestId, details: RequestDetails) => {
        this.buckets[this.buckets.length - 1].addSummary(details)
        this.inFlightRequestStates.delete(id)
    }
    onRequestStateChange = (id: RequestId, newState: RequestState) => {
        this.inFlightRequestStates.set(id, newState)
    }

    run(): void {
        // window shift loop
        setInterval(() => {
            this.buckets.push(new BucketSummary())
            if (this.buckets.length > this.bucketsPerWindow + 1) {
                this.buckets.shift()
            }

            console.log(this.getSummary())

        }, 1000 * this.windowSeconds / this.bucketsPerWindow)
    }
}
