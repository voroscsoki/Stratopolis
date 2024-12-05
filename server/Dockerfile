FROM gradle:8.11.1-jdk21 AS build

WORKDIR /app

COPY . .

WORKDIR /app/server

RUN gradle build --no-daemon

# Create a second stage for a lighter runtime
FROM openjdk:21-slim

WORKDIR /app

COPY --from=build /app/server/build/libs/server-all.jar server-all.jar
COPY --from=build /app/server/config.json config.json
COPY --from=build /app/server/distributions.json distributions.json

EXPOSE 8085

VOLUME ["/app"]

ENTRYPOINT ["java", "-jar", "server-all.jar"]
