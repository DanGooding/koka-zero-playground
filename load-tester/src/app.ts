import commandLineArgs from 'command-line-args'

function main(options: any) {
    console.log(options);
    const ws = new WebSocket(options.url);

    ws.onopen = () => {
        console.log('connected');
        ws.send(JSON.stringify({
            '@type': 'compile-and-run',
            'code': 'fun main() { println-int(3 * 4); }'
        }));
    }

    ws.onmessage = (e) => {
        const message = JSON.parse(e.data);
        console.log('received message', message);
    }

    ws.onclose = () => {
        console.log('closed');
    }
}

const optionDefinitions = [
    { name: 'url', type: String, required: true },
]

main(commandLineArgs(optionDefinitions))
