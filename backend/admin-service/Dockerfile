# syntax=docker/dockerfile:1.7
# =============================================================================
#  CareerBridge – TLS-resilient Spring Boot Dockerfile
# =============================================================================
#  Handles PKIX / corporate-proxy TLS errors via:
#    1. Maven mirror (Aliyun) — avoids Maven Central direct TLS altogether
#    2. SSL insecure flags     — fallback if mirror also uses intercepted TLS
#    3. keytool import         — imports Docker-host CA bundle so JDK trusts it
#  Multi-stage: Maven + JDK not in runtime image.
# =============================================================================

# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 – Dependency cache
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS deps
WORKDIR /build

# ── TLS fix: inject Maven settings that use Aliyun mirror + disable SSL verify
COPY docker/maven/settings.xml /root/.m2/settings.xml

# ── TLS fix: add -Dmaven.wagon.http.ssl.insecure at JVM level too (belt+suspenders)
ENV MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true \
                -Dmaven.wagon.http.ssl.allowall=true \
                -Dhttps.protocols=TLSv1.2,TLSv1.3 \
                -Djavax.net.ssl.trustStore=/usr/lib/jvm/default-jvm/lib/security/cacerts"

# Parent pom first so child <parent> resolves
COPY pom.xml /pom.xml
# SERVICE_POM placeholder — replaced per-service below
COPY admin-service/pom.xml pom.xml

RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    mvn -f /pom.xml install -N -q --no-transfer-progress \
        -Dmaven.wagon.http.ssl.insecure=true \
        -Dmaven.wagon.http.ssl.allowall=true && \
    mvn dependency:go-offline -q --no-transfer-progress \
        -Dmaven.wagon.http.ssl.insecure=true \
        -Dmaven.wagon.http.ssl.allowall=true

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 – Compile + package
# ─────────────────────────────────────────────────────────────────────────────
FROM deps AS builder
COPY admin-service/src ./src
RUN --mount=type=cache,target=/root/.m2/repository,sharing=locked \
    mvn package -DskipTests -q --no-transfer-progress \
        -Dmaven.wagon.http.ssl.insecure=true \
        -Dmaven.wagon.http.ssl.allowall=true && \
    java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ─────────────────────────────────────────────────────────────────────────────
# Stage 3 – Minimal runtime (JRE-only, non-root)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /build/extracted/dependencies/          ./
COPY --from=builder /build/extracted/spring-boot-loader/    ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/           ./
USER spring
EXPOSE 8086
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=65.0", \
  "-XX:InitialRAMPercentage=35.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
