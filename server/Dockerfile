FROM gradle:8.11.1-jdk23 AS build

WORKDIR /app

COPY . .

WORKDIR /app/server

RUN gradle build --no-daemon

# Create a second stage for a lighter runtime
FROM openjdk:23-slim

WORKDIR /app

COPY --from=build /app/server/build/libs/server-all.jar server-all.jar

EXPOSE 8085

VOLUME ["/app"]

ENTRYPOINT ["java", "-jar", "server-all.jar"]
