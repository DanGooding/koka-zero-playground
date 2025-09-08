import commandLineArgs from 'command-line-args'
import {sendRequest} from './requestor.js'
import LoadTester from './loadtester.js'
import type {RequestDetails} from './outcomes.js'

type cli_params = {
    url: URL,
    requestsPerSecond: number,
    printRawStats: boolean,
}

function main(options: cli_params) {
    const requestor =
        (onOutcome: (summary: RequestDetails) => void) => sendRequest(options.url, onOutcome)

    const loadTester = new LoadTester(options.requestsPerSecond, requestor, options.printRawStats)
    loadTester.run()
}

function cli() {
    const optionDefinitions = [
        { name: 'url', type: (url: string) => new URL(url) },
        { name: 'rps', type: Number, defaultValue: 1 },
        { name: 'print-raw-stats', type: Boolean, defaultValue: false },
    ]

    const options = commandLineArgs(optionDefinitions)
    const params: cli_params = {
        url: options.url,
        requestsPerSecond: options.rps,
        printRawStats: options['print-raw-stats']
    }
    main(params)
}

cli()
