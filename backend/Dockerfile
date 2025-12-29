# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /project

# Copier uniquement les fichiers de dépendances en premier (pour meilleur cache)
COPY pom.xml mvnw /project/
COPY .mvn /project/.mvn

# Pré-télécharger les dépendances
RUN mvn -B -ntp -DskipTests dependency:go-offline

# Copier les sources APRÈS les dépendances
COPY src src

# Build
RUN mvn -B -ntp package -DskipTests -Dquarkus.package.type=fast-jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work

# Créer un utilisateur non-root
RUN addgroup -g 1000 app && adduser -D -u 1000 -G app app

# Copier les artefacts depuis le build
COPY --from=build --chown=app:app /project/target/quarkus-app/app /work/app
COPY --from=build --chown=app:app /project/target/quarkus-app/quarkus /work/quarkus
COPY --from=build --chown=app:app /project/target/quarkus-app/lib /work/lib
COPY --from=build --chown=app:app /project/target/quarkus-app/quarkus-run.jar /work/application.jar

# Permissions appropriées
RUN chmod 755 /work

# Switcher vers l'utilisateur non-root
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/work/application.jar", "-Dquarkus.http.host=0.0.0.0"]
