# syntax=docker.io/docker/dockerfile:1.7-labs

ARG JAVA_BUILD_IMAGE=maven:3.9.11-eclipse-temurin-21-alpine
ARG RUN_IMAGE=alpine:3.22
ARG RUN_COMPILER_IMAGE=ghcr.io/dangooding/koka-zero:main
ARG NODE_BUILD_IMAGE=node:22-alpine3.22
ARG NGINX_IMAGE=jonasal/nginx-certbot:6.0.1-nginx1.29.1-alpine

FROM $JAVA_BUILD_IMAGE AS package
WORKDIR /build

# need to copy all modules in, since maven expects to find all pom files specified in the root pom
COPY --exclude=client/  . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -X

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

RUN for project in compile-service-app runner-service-app compile-and-run-service-app; do \
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout --projects app/"$project"); \
    mv \
      "./app/$project/target/$project-$version".war \
      $project.war ; \
    done

FROM $RUN_COMPILER_IMAGE AS run-koka-compiler

FROM run-koka-compiler AS koka-playground-compile-service
WORKDIR /app

RUN apk add openjdk21-jre-headless
RUN apk add jq

COPY --from=package /build/compile-service-app.war app.war

CMD [ "java", "-jar", "app.war", \
    "--compiler.exe-path=/app/koka-zero", \
    "--compiler.koka-zero-config-path=/app/koka-zero-config.sexp" ]

FROM $RUN_IMAGE AS koka-playground-runner-service
WORKDIR /app

RUN apk add openjdk21-jre-headless
RUN apk add bubblewrap
RUN apk add jq

COPY --from=package /build/runner-service-app.war app.war
COPY --from=run-koka-compiler /usr/local/lib/* /usr/local/lib/

CMD [ "java", "-jar", "app.war", \
    "--runner.bubblewrap-path=/usr/bin/bwrap" ]

FROM $RUN_IMAGE AS koka-playground-compile-and-run-service
WORKDIR /app

RUN apk add openjdk21-jre-headless
RUN apk add jq

COPY --from=package /build/compile-and-run-service-app.war app.war

CMD [ "java", "-jar", "app.war" ]

FROM $NODE_BUILD_IMAGE AS frontend-build
WORKDIR /build

COPY client/package.json client/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm clean-install

COPY --exclude=client/nginx.conf \
  client/ ./

RUN npm run build

FROM $NGINX_IMAGE AS koka-playground-proxy

COPY client/nginx.conf /etc/nginx/user_conf.d/
COPY --from=frontend-build /build/dist /data/www
