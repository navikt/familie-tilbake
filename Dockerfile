FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=TRUE
ENV APP_NAME=familie-tilbake

COPY ./target/familie-tilbake.jar "app.jar"
