FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY tictac-common ./tictac-common
COPY game-session-service ./game-session-service
COPY game-engine-service/build.gradle ./game-engine-service/build.gradle
COPY ui/build.gradle ./ui/build.gradle
RUN mkdir -p game-engine-service/src ui/src
RUN ./gradlew :game-session-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/game-session-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]