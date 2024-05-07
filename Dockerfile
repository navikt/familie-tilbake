# Final image
FROM gcr.io/distroless/java21-debian12:nonroot
COPY --chown=nonroot:nonroot ./target/familie-tilbake.jar /app/app.jar
WORKDIR /app

ENV APP_NAME=familie-tilbake
ENV TZ="Europe/Oslo"
# TLS Config works around an issue in OpenJDK... See: https://github.com/kubernetes-client/java/issues/854
ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "/app/app.jar", "-XX:MinRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError" ]