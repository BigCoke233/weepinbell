FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/weepinbell-0.1.0-SNAPSHOT-standalone.jar app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]

