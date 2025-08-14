FROM maven:3.9.11-eclipse-temurin-21-alpine AS package
WORKDIR /build

# need to copy all modules in, since maven expects to find all pom files specified in the root pom
COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -X --projects common,compile-service,runner-service,compile-and-run-service

RUN --mount=type=cache,target=/root/.m2 \
    mvn package --projects compile-service,runner-service,compile-and-run-service -DskipTests

RUN for project in compile-service runner-service compile-and-run-service; do \
    version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout --projects "$project"); \
    mv \
      "./$project/target/$project-$version".war \
      $project.war ; \
    done

FROM koka-compiler-image AS koka-playground-compile-service
WORKDIR /app

RUN apk add openjdk21-jre-headless

COPY --from=package /build/compile-service.war app.war

EXPOSE 8080
ENTRYPOINT [ "java", \
    "-jar", "app.war", \
    "--compiler.exe-path=/app/koka-zero", \
    "--compiler.koka-zero-config-path=/app/koka-zero-config.sexp" ]

FROM alpine:3.22 AS koka-playground-runner-service
WORKDIR /app

RUN apk add openjdk21-jre-headless
RUN apk add bubblewrap

COPY --from=package /build/runner-service.war app.war
COPY --from=koka-compiler-image /usr/local/lib/* /usr/local/lib/

EXPOSE 8080
ENTRYPOINT [ "java", \
    "-jar", "app.war", \
    "--runner.bubblewrap-path=/usr/bin/bwrap" ]

FROM alpine:3.22 AS koka-playground-compile-and-run-service
WORKDIR /app

RUN apk add openjdk21-jre-headless

COPY --from=package /build/compile-and-run-service.war app.war

EXPOSE 8080
ENTRYPOINT [ "java", \
    "-jar", "app.war" ]
