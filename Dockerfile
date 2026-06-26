FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/reconcilliation-service-1.0.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
