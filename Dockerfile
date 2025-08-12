# based on https://docs.docker.com/guides/java/containerize/

FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

# Download dependencies as a separate step to take advantage of Docker's caching.
# Leverage a cache mount to /root/.m2 so that subsequent builds don't have to
# re-download packages.
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -DskipTests

FROM deps AS package
WORKDIR /build

COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    mv \
      target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).war \
      target/app.war

FROM ghcr.io/dangooding/koka-zero:main AS app
WORKDIR /app

COPY --from=package /build/target/app.war ./

RUN apk update
RUN apk add bubblewrap

RUN apk add openjdk21-jre-headless

EXPOSE 8080
ENTRYPOINT [ "java", \
    "-jar", "app.war", \
    "--compiler.exe-path=/app/koka-zero", \
    "--compiler.koka-zero-config-path=/app/koka-zero-config.sexp", \
    "--runner.bubblewrap-path=/usr/bin/bwrap" ]
