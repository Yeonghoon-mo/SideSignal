# 빌드 단계
FROM gradle:8.13-jdk21-alpine AS builder

WORKDIR /app
COPY ./ /app
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --stacktrace --no-daemon

# 실행 단계
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r svc && useradd --no-log-init -r -g svc svc

COPY --from=builder --chown=svc:svc /app/build/libs/SideSignal-Server-0.0.1-SNAPSHOT.jar /app/sidesignal-server.jar

USER svc

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/sidesignal-server.jar"]
