ARG JAVA_BUILD_IMAGE=maven:3.9.11-eclipse-temurin-21-alpine
ARG RUN_IMAGE=alpine:3.22
ARG RUN_COMPILER_IMAGE=koka-compiler-image

FROM $JAVA_BUILD_IMAGE AS package
WORKDIR /build

# need to copy all modules in, since maven expects to find all pom files specified in the root pom
COPY . .

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

FROM $RUN_COMPILER_IMAGE AS koka-playground-compile-service
WORKDIR /app

RUN apk add openjdk21-jre-headless

COPY --from=package /build/compile-service-app.war app.war

EXPOSE 8080
ENTRYPOINT [ "java" ]
CMD [ "-jar", "app.war", \
    "--compiler.exe-path=/app/koka-zero", \
    "--compiler.koka-zero-config-path=/app/koka-zero-config.sexp" ]

FROM $RUN_IMAGE AS koka-playground-runner-service
WORKDIR /app

RUN apk add openjdk21-jre-headless
RUN apk add bubblewrap

COPY --from=package /build/runner-service-app.war app.war
COPY --from=koka-compiler-image /usr/local/lib/* /usr/local/lib/

EXPOSE 8080
ENTRYPOINT [ "java" ]
CMD [ "-jar", "app.war", \
    "--runner.bubblewrap-path=/usr/bin/bwrap" ]

FROM $RUN_IMAGE AS koka-playground-compile-and-run-service
WORKDIR /app

RUN apk add openjdk21-jre-headless

COPY --from=package /build/compile-and-run-service-app.war app.war

EXPOSE 8080
ENTRYPOINT [ "java" ]
CMD [ "-jar", "app.war" ]
