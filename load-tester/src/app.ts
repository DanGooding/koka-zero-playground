import commandLineArgs from 'command-line-args'
import { sendCompileAndRunRequest, sendCompileRequest } from './requestor.js'
import LoadTester from './loadtester.js'
import type {RequestDetails} from './outcomes.js'

type cli_params = {
    url: URL,
    requestsPerSecond: number,
    printRawStats: boolean,
    service: 'compile-and-run' | 'compile'
}

function main(options: cli_params) {
    let sendRequest;
    switch (options.service) {
        case 'compile-and-run':
            sendRequest = sendCompileAndRunRequest
            break
        case 'compile':
            sendRequest = sendCompileRequest
            break
        default:
            throw new Error(`Unsupported service: ${options.service}`)
    }
    // TODO: requestor should be a class with a request() method
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
        { name: 'service', type: String }
    ]

    const options = commandLineArgs(optionDefinitions)
    const params: cli_params = {
        url: options.url,
        requestsPerSecond: options.rps,
        printRawStats: options['print-raw-stats'],
        service: options.service
    }
    main(params)
}

cli()
