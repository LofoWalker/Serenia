# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /project

COPY pom.xml /project/
RUN mvn -B -ntp -DskipTests dependency:go-offline
COPY src src

RUN mvn -B -ntp package -DskipTests -Dquarkus.package.type=fast-jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work

RUN addgroup -g 1000 app && adduser -D -u 1000 -G app app

COPY --from=build --chown=app:app /project/target/quarkus-app/app /work/app
COPY --from=build --chown=app:app /project/target/quarkus-app/quarkus /work/quarkus
COPY --from=build --chown=app:app /project/target/quarkus-app/lib /work/lib
COPY --from=build --chown=app:app /project/target/quarkus-app/quarkus-run.jar /work/application.jar

# Entrypoint
COPY docker-entrypoint.sh /work/docker-entrypoint.sh
RUN chmod 775 /work /work/docker-entrypoint.sh

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "/work/application.jar"]