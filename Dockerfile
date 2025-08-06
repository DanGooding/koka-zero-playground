FROM alpine:3.22 as has-bubblewrap

RUN apk update
RUN apk add bubblewrap

CMD sleep inf
