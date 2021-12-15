FROM navikt/java:17
ENV APP_NAME=familie-tilbake
COPY ./target/familie-tilbake.jar "app.jar"
