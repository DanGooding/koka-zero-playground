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

export class CompileAndRunRequestor extends Requestor {
    constructor(url: URL) {
        super(url)
    }

    request(onComplete: (summary: RequestDetails) => void) {
        const ws = new WebSocket(this.url);

        const openTime = Date.now()
        const toEventSeconds = new Map<(RequestEvent | 'first-response' | 'requested-run' | 'running'), number>()

        ws.onopen = () => {
            toEventSeconds.set('opened', secondsSince(openTime))
            ws.send(JSON.stringify({
                '@type': 'compile-and-run',
                'code': 'fun main() { println-int(3 * 4); }'
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
    constructor(url: URL) {
        super(url);
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
