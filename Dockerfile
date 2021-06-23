FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=true
ENV APP_NAME=familie-tilbake
COPY init.sh /init-scripts/init.sh
COPY ./target/familie-tilbake.jar "app.jar"
