import type {RequestDetails, RequestEvent, RequestId, RequestOutcome, RequestState} from './outcomes.js';
import Requestor, {secondsSince} from './requestor.js';
import http from 'http'

export default class CompileRequestor extends Requestor {
    constructor(url: URL, requestVariant: string) {
        super(url);

        if (requestVariant !== 'default') {
            throw new Error(`Unsupported request variant: ${requestVariant}`)
        }
    }

    request(
        id: RequestId,
        onStateChange: (id: RequestId, newState: RequestState) => void,
        onComplete: (id: RequestId, summary: RequestDetails) => void): RequestState {
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
            onStateChange(id, 'connected')
        })
        req.on('error', () => {
            outcome = 'transport error';
        })
        req.on('close', () => {
            toEventSeconds.set('closed', secondsSince(openTime))
            onStateChange(id, 'closed')
            onComplete(id, {
                outcome: outcome ?? 'server error',
                toEventSeconds
            })
        })

        req.write(postData);
        req.end();
        return 'sent-request'
    }
}
