FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system nexus && useradd --system --gid nexus nexus
COPY --from=build /app/target/nexus-verde-1.0.0.jar app.jar
USER nexus
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=55", "-jar", "/app/app.jar"]
