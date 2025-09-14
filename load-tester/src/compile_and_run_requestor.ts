import type {RequestDetails, RequestEvent, RequestId, RequestOutcome, RequestState} from './outcomes.js';
import Requestor, {secondsSince} from './requestor.js';

const compileAndRunRequestVariants = ['trivial', 'high-cpu', 'long-blocking'] as const
type CompileAndRunRequestVariant = typeof compileAndRunRequestVariants[number]

type RequestPayload = {
    code: string,
    stdin?: { sleepMS: number, write: string }
}

export default class CompileAndRunRequestor extends Requestor {
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

    requestPayload(): RequestPayload {
        switch (this.requestVariant) {
            case 'trivial': {
                const code = 'fun main() { println-int(3 * 4); }'
                return { code }
            }
            case 'high-cpu': {
                const code = `
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
                return { code }
            }
            case 'long-blocking': {
                const code = 'fun main() { println-int(read-int(())); }'
                const stdin = {sleepMS: 100, write: '5\n'}
                return {code, stdin}
            }
        }
    }

    request(
        id: RequestId,
        onStateChange: (id: RequestId, newState: RequestState) => void,
        onComplete: (id: RequestId, summary: RequestDetails) => void): RequestState {
        const ws = new WebSocket(this.url);

        const openTime = Date.now()
        const toEventSeconds = new Map<(RequestEvent | 'first-response' | 'requested-run' | 'running'), number>()

        const {code, stdin} = this.requestPayload()

        ws.onopen = () => {
            toEventSeconds.set('opened', secondsSince(openTime))
            onStateChange(id, 'opened')
            ws.send(JSON.stringify({
                '@type': 'compile-and-run',
                'code': code
            }));
        }

        const onClose = (outcome: RequestOutcome) => {
            if (toEventSeconds.has('closed')) return;

            toEventSeconds.set('closed', secondsSince(openTime))
            onStateChange(id, 'closed')
            onComplete(id, { toEventSeconds, outcome });
        }

        ws.onmessage = (e) => {
            const secondsToNow = secondsSince(openTime)
            if (!toEventSeconds.has('first-response')) {
                toEventSeconds.set('first-response', secondsToNow)
                onStateChange(id, 'first-response')
            }
            const message = JSON.parse(e.data);
            if (message['@type'] === 'error') {
                onClose('client error')
            }else if (message['@type'] === 'starting-run') {
                toEventSeconds.set('requested-run', secondsToNow)
                onStateChange(id, 'requested-run')
            }else if (message['@type'] === 'running') {
                toEventSeconds.set('running', secondsToNow)
                onStateChange(id, 'running')

                if (stdin != null) {
                    const {sleepMS, write} = stdin
                    setTimeout(() => {
                            ws.send(JSON.stringify({
                                '@type': 'stdin',
                                'content': write
                            }))
                        },
                        sleepMS)
                }
            }
        }

        ws.onclose = (e) => {
            // ok is NORMAL or GOING_AWAY
            const closeOk = e.code == 1000 || e.code == 1001;
            onClose(closeOk ? 'ok' : 'server error');
        }

        return 'connecting'
    }

}

