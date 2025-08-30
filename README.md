# *Koka Zero* Language Playground

An online environment to write, run, and learn Koka. Uses my
implementation [KokaZero](https://github.com/DanGooding/koka-zero/).

Built with Java Spring, with [Bubblewrap](https://github.com/containers/bubblewrap) to sandbox user binaries.

Orchestrated with Docker Swarm, atop a NixOS DigitalOcean droplet.

## Local Development

Startup the backend services

```shell
docker compose up --build
```

Run the frontend with Vite dev mode

```shell
cd client/
npm i
npm run dev
```
