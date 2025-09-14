import type { RequestId } from './outcomes.js'
import { Requestor } from './requestor.js'
import Aggregator from './aggregator.js'

export default class LoadTester {
    readonly requestsPerSecond: number
    readonly requestor: Requestor

    readonly windowSeconds: number = 10
    readonly bucketsPerWindow: number = 10

    aggregator: Aggregator

    constructor(requestsPerSecond: number, requestor: Requestor, printRawStats: boolean = false) {
        this.requestsPerSecond = requestsPerSecond
        this.requestor = requestor

        this.aggregator = new Aggregator(printRawStats)
    }

    run(): void {
        this.aggregator.run()

        let nextId: RequestId = 0
        // request loop
        setInterval(() => {
            const id = nextId++
            const initialState =
                this.requestor.request(id, this.aggregator.onRequestStateChange, this.aggregator.onRequestComplete);
            this.aggregator.onRequestStateChange(id, initialState)
        }, 1000 / this.requestsPerSecond)
    }
}
