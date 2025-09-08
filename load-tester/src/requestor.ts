import type { RequestDetails, RequestOutcome, RequestEvent } from './outcomes.js'

export function sendRequest(
    url: URL,
    onComplete: (requestDetails: RequestDetails) => void,
) {
    const ws = new WebSocket(url);

    const openTime = Date.now()
    const toEventSeconds = new Map<RequestEvent, number>()

    ws.onopen = () => {
        toEventSeconds.set('opened', (Date.now() - openTime) / 1000)
        ws.send(JSON.stringify({
            '@type': 'compile-and-run',
            'code': 'fun main() { println-int(3 * 4); }'
        }));
    }

    const onClose = (outcome: RequestOutcome) => {
        if (toEventSeconds.has('closed')) return;

        toEventSeconds.set('closed', (Date.now() - openTime) / 1000)
        onComplete({ toEventSeconds, outcome });
    }

    ws.onmessage = (e) => {
        const secondsToNow = (Date.now() - openTime) / 1000
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
