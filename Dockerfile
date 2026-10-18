# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --uid 1001 app
WORKDIR /work
COPY --from=build /workspace/target/content-helper-*.jar /app/app.jar
USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
