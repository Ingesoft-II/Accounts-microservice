# ---- Etapa 1: build --------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY mvnw .mvn/ ./
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests package

# ---- Etapa 2: runtime ------------------------------------------------
FROM eclipse-temurin:21-jre
ARG PORT=8081
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system kp \
    && useradd --system --gid kp --home-dir /app kp \
    && mkdir -p /keys \
    && chown -R kp:kp /keys
WORKDIR /app
COPY --from=build /build/target/cuentas.jar app.jar
USER kp
EXPOSE ${PORT}
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
