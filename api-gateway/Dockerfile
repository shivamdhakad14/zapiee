FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 3333

ENTRYPOINT ["java", "-jar", "app.jar"]