FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY . .

WORKDIR /app/server

RUN gradle build --no-daemon

# Create a second stage for a lighter runtime
FROM openjdk:21-slim

WORKDIR /app

COPY --from=build /app/server/build/libs/server-all.jar server-all.jar

EXPOSE 8085

VOLUME ["/app"]

ENTRYPOINT ["java", "-jar", "server-all.jar"]