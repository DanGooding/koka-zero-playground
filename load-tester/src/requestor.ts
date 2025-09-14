import type { RequestDetails, RequestOutcome, RequestEvent } from './outcomes.js'
import http from 'http'

function secondsSince(then: number): number {
    return (Date.now() - then) / 1000
}

export abstract class Requestor {
    url: URL

    constructor(url: URL) {
        this.url = url
    }

    abstract request(onComplete: (summary: RequestDetails) => void): void
}

const compileAndRunRequestVariants = ['trivial', 'high-cpu'] as const
type CompileAndRunRequestVariant = typeof compileAndRunRequestVariants[number]

export class CompileAndRunRequestor extends Requestor {
    readonly requestVariant: CompileAndRunRequestVariant

    constructor(url: URL, requestVariant: string) {
        super(url)

        const variant =
            compileAndRunRequestVariants.find(v => v === requestVariant)

        if (variant) {
            this.requestVariant = variant
        }else {
            throw new Error(`Unsupported request variant: ${requestVariant}`)
        }
    }

    payloadCode(): string {
        switch (this.requestVariant) {
            case 'trivial':
                return 'fun main() { println-int(3 * 4); }'
            case 'high-cpu':
                return `
fun fib(n) {
  if n == 0
  then 0
  else if n == 1
  then 1
  else fib(n - 1) + fib(n - 2);
};

fun main() {
  fib(40).print-int();
};
`
        }
    }

    request(onComplete: (summary: RequestDetails) => void): void {
        const ws = new WebSocket(this.url);

        const openTime = Date.now()
        const toEventSeconds = new Map<(RequestEvent | 'first-response' | 'requested-run' | 'running'), number>()

        ws.onopen = () => {
            toEventSeconds.set('opened', secondsSince(openTime))
            ws.send(JSON.stringify({
                '@type': 'compile-and-run',
                'code': this.payloadCode()
            }));
        }

        const onClose = (outcome: RequestOutcome) => {
            if (toEventSeconds.has('closed')) return;

            toEventSeconds.set('closed', secondsSince(openTime))
            onComplete({ toEventSeconds, outcome });
        }

        ws.onmessage = (e) => {
            const secondsToNow = secondsSince(openTime)
            if (!toEventSeconds.has('first-response')) {
                toEventSeconds.set('first-response', secondsToNow)
            }
            const message = JSON.parse(e.data);
            if (message['@type'] === 'error') {
                onClose('client error')
            }else if (message['@type'] === 'starting-run') {
                toEventSeconds.set('requested-run', secondsToNow)
            }else if (message['@type'] === 'running') {
                toEventSeconds.set('running', secondsToNow)
            }
        }

        ws.onclose = (e) => {
            // ok is NORMAL or GOING_AWAY
            const closeOk = e.code == 1000 || e.code == 1001;
            onClose(closeOk ? 'ok' : 'server error');
        }
    }

}

export class CompileRequestor extends Requestor {
    constructor(url: URL, requestVariant: string) {
        super(url);

        if (requestVariant !== 'default') {
            throw new Error(`Unsupported request variant: ${requestVariant}`)
        }
    }

    request(onComplete: (summary: RequestDetails) => void) {
        const postData = JSON.stringify({code: 'fun main() { println-int(5 * 6); }'})

        if (this.url.search) {
            throw new Error('url query params not implemented: ' + this.url.search);
        }
        const options = {
            host: this.url.hostname,
            port: this.url.port,
            path: this.url.pathname,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(postData),
            },
        };

        const openTime = Date.now()
        const toEventSeconds = new Map<RequestEvent | 'connected', number>()
        let outcome: RequestOutcome

        const req = http.request(options, (res) => {
            const statusCode = res.statusCode ?? 0;
            if (statusCode >= 200 && statusCode < 300) {
                outcome = 'ok';
            }else if (statusCode >= 500 && statusCode < 600) {
                outcome = 'server error';
            }else {
                outcome = 'client error';
            }

            res.on('data', (chunk) => {
                if (outcome) return;

                // responses are small - assume they fit in one chunk
                const result = JSON.parse(chunk.toString())
                if (!result['ok']) {
                    outcome = 'client error';
                }
            })
        })

        req.on('socket', () => {
            toEventSeconds.set('connected', secondsSince(openTime))
        })
        req.on('error', () => {
            outcome = 'transport error';
        })
        req.on('close', () => {
            toEventSeconds.set('closed', secondsSince(openTime))
            onComplete({
                outcome: outcome ?? 'server error',
                toEventSeconds
            })
        })

        req.write(postData);
        req.end();
    }
}
