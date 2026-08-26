FROM maven:3.9-eclipse-temurin-25 AS dev
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]

FROM dev AS build
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre AS prod
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
