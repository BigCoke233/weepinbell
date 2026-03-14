FROM clojure:lein-alpine AS builder

WORKDIR /app

COPY project.clj /app/
RUN lein deps

COPY . /app
RUN lein uberjar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/default+uberjar/weepinbell-v0.1.0-standalone.jar ./weepinbell.jar

EXPOSE 3000

CMD ["java", "-jar", "weepinbell.jar"]

