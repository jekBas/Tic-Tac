FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY tictac-common ./tictac-common
COPY game-engine-service ./game-engine-service
COPY game-session-service/build.gradle ./game-session-service/build.gradle
COPY ui/build.gradle ./ui/build.gradle
RUN mkdir -p game-session-service/src ui/src
RUN ./gradlew :game-engine-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/game-engine-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]