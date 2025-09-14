import commandLineArgs from 'command-line-args'
import Requestor from './requestor.js'
import CompileRequestor from './compile_requestor.js'
import CompileAndRunRequestor from './compile_and_run_requestor.js'
import LoadTester from './loadtester.js'

type cli_params = {
    url: URL,
    requestsPerSecond: number,
    printRawStats: boolean,
    requestor: Requestor,
}

function main(options: cli_params) {
    const loadTester = new LoadTester(options.requestsPerSecond, options.requestor, options.printRawStats)
    loadTester.run()
}

function cli() {
    const optionDefinitions = [
        { name: 'url', type: (url: string) => new URL(url) },
        { name: 'rps', type: Number, defaultValue: 1 },
        { name: 'print-raw-stats', type: Boolean, defaultValue: false },
        { name: 'service', type: String },
        { name: 'request-variant', type: String },
    ]

    const options = commandLineArgs(optionDefinitions)

    let requestor: Requestor;
    switch (options.service) {
        case 'compile-and-run':
            requestor = new CompileAndRunRequestor(options.url, options['request-variant'])
            break
        case 'compile':
            requestor = new CompileRequestor(options.url, options['request-variant'])
            break
        default:
            throw new Error(`Unsupported service: ${options.service}`)
    }

    const params: cli_params = {
        url: options.url,
        requestsPerSecond: options.rps,
        printRawStats: options['print-raw-stats'],
        requestor
    }
    main(params)
}

cli()
