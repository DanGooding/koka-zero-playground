import type { RequestDetails, RequestOutcome } from './outcomes.js'

export function sendRequest(
    url: URL,
    onComplete: (requestDetails: RequestDetails) => void,
) {
    const ws = new WebSocket(url);

    const openTime = Date.now()
    let requestDetails: Partial<RequestDetails> = {}

    ws.onopen = () => {
        requestDetails.toOpenSeconds = (Date.now() - openTime) / 1000;
        ws.send(JSON.stringify({
            '@type': 'compile-and-run',
            'code': 'fun main() { println-int(3 * 4); }'
        }));
    }

    const onClose = (outcome: RequestOutcome) => {
        if (requestDetails.outcome) return; // already closed
        requestDetails.toCloseSeconds = (Date.now() - openTime) / 1000;
        onComplete({ ...requestDetails, outcome: outcome });
    }

    ws.onmessage = (e) => {
        const secondsToNow = (Date.now() - openTime) / 1000
        if (requestDetails.toFirstResponseSeconds == null) {
            requestDetails.toFirstResponseSeconds = secondsToNow
        }
        const message = JSON.parse(e.data);
        if (message['@type'] === 'error') {
            onClose('client error')
        }else if (message['@type'] === 'starting-run') {
            requestDetails.toRequestedRunSeconds = secondsToNow
        }else if (message['@type'] === 'running') {
            requestDetails.toRunningSeconds = secondsToNow
        }
    }

    ws.onclose = (e) => {
        // ok is NORMAL or GOING_AWAY
        const closeOk = e.code == 1000 || e.code == 1001;
        onClose(closeOk ? 'ok' : 'server error');
    }
}
