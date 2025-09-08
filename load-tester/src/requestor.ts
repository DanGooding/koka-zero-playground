import type { RequestDetails, RequestOutcome } from './outcomes.js'

export function sendRequest(
    url: URL,
    onComplete: (RequestDetails: RequestDetails) => void,
) {
    const ws = new WebSocket(url);

    const openTime = Date.now()
    let RequestDetails: Partial<RequestDetails> = {}

    ws.onopen = () => {
        RequestDetails.toOpenMs = Date.now() - openTime;
        ws.send(JSON.stringify({
            '@type': 'compile-and-run',
            'code': 'fun main() { println-int(3 * 4); }'
        }));
    }

    const onClose = (outcome: RequestOutcome) => {
        if (RequestDetails.outcome) return; // already closed
        RequestDetails.toCloseMs = Date.now() - openTime;
        onComplete({ ...RequestDetails, outcome: outcome });
    }

    ws.onmessage = (e) => {
        RequestDetails.toFirstResponseMs = Date.now() - openTime;
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
