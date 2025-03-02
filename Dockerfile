# Final image
FROM gcr.io/distroless/java21-debian12:nonroot
COPY --chown=nonroot:nonroot ./build/libs/familie-tilbake.jar /app/app.jar
WORKDIR /app

ENV APP_NAME=familie-tilbake
ENV TZ="Europe/Oslo"
ENTRYPOINT [ "java", "-jar", "/app/app.jar", "-XX:MinRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError" ]
