# Run the JAR produced by `mvn clean package`
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/cpu-scheduling-simulator.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
