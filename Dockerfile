# Image for the family HTTP API. The desktop app is not part of it.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
# Tests run in CI before the image is built.
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
COPY --from=build /build/target/lib lib
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "api"]
