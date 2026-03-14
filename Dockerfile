FROM clojure:lein-alpine AS builder

WORKDIR /app
COPY project.clj /app/
RUN lein deps

COPY . /app
RUN lein uberjar && \
    cp target/default+uberjar/*-standalone.jar ./app.jar || \
    cp target/*-standalone.jar ./app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/app.jar ./weepinbell.jar

EXPOSE 3000
CMD ["java", "-jar", "weepinbell.jar"]
