import commandLineArgs from 'command-line-args'

type RequestOutcome = 'ok' | 'client error' | 'server error'
type RequestSummary = {
    outcome: RequestOutcome
    toOpenMs?: number,
    toFirstResponseMs?: number,
    toCloseMs?: number,
}

function send_request(
    url: URL,
    onComplete: (requestSummary: RequestSummary) => void,
) {
    const ws = new WebSocket(url);

    const openTime = Date.now()
    let requestSummary: Partial<RequestSummary> = {}

    ws.onopen = () => {
        requestSummary.toOpenMs = Date.now() - openTime;
        ws.send(JSON.stringify({
            '@type': 'compile-and-run',
            'code': 'fun main() { println-int(3 * 4); }'
        }));
    }

    const onClose = (outcome: RequestOutcome) => {
        if (requestSummary.outcome) return; // already closed
        requestSummary.toCloseMs = Date.now() - openTime;
        onComplete({ ...requestSummary, outcome: outcome });
    }

    ws.onmessage = (e) => {
        requestSummary.toFirstResponseMs = Date.now() - openTime;
        const message = JSON.parse(e.data);
        if (message['@type'] === 'error') {
            onClose('client error')
        }
    }

    ws.onclose = (e) => {
        // ok is NORMAL or GOING_AWAY
        const closeOk = e.code == 1000 || e.code == 1001;
        onClose(closeOk ? 'ok' : 'server error');
    }
}

type cli_params = {
    url: URL,
    requests_per_second: number,
}

function main(options: cli_params) {

    let totalCount = 0
    const outcomeCounts = new Map<RequestOutcome, number>()

    const onComplete = (summary: RequestSummary) => {
        outcomeCounts.set(summary.outcome,
            (outcomeCounts.get(summary.outcome) ?? 0) + 1);
        totalCount += 1
    }

    setInterval(() => {
        send_request(options.url, onComplete)
    }, 1000 / options.requests_per_second)

    const updatesPerSecond = 5
    setInterval(() => {
        const outcomeFractions: [RequestOutcome, string][] =
            Array.from(outcomeCounts.entries())
                .map(
                    ([outcome, count]: [RequestOutcome, number]) => {
                        return [outcome, (count / totalCount * 100).toFixed(1)];
                    })
        console.log(new Map<RequestOutcome, string>(outcomeFractions))

    }, 1000 / updatesPerSecond)
}

function cli() {
    const optionDefinitions = [
        { name: 'url', type: (url: string) => new URL(url) },
        { name: 'rps', type: Number, defaultValue: 1 },
    ]

    const options = commandLineArgs(optionDefinitions)
    const params: cli_params = {
        url: options.url,
        requests_per_second: options.rps,
    }
    main(params)
}

cli()
