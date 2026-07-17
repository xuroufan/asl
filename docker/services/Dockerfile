FROM eclipse-temurin:21-jre

ARG SERVICE
ARG PORT
ARG EXTRA_EXCLUDES=

LABEL org.opencontainers.image.source="https://github.com/xuroufan/asl"
LABEL org.opencontainers.image.description="ASL Futures Trading Platform - ${SERVICE}"
LABEL org.opencontainers.image.version="1.0.1"

WORKDIR /app

COPY backend/${SERVICE}/target/${SERVICE}-1.0.0-SNAPSHOT.jar app.jar
COPY config/health-fix.yml config/health-fix.yml
COPY docker/services/entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

EXPOSE ${PORT}

ENV EXTRA_EXCLUDES=${EXTRA_EXCLUDES}

ENTRYPOINT ["/app/entrypoint.sh"]
