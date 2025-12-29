# Build stage - Quarkus native (GraalVM)
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-java21 AS build
WORKDIR /project

# Optionnel: éviter les prompts + s'assurer des paquets requis pour l'édition de liens
USER root
RUN microdnf install -y findutils && microdnf clean all
USER 1001

# Copier d'abord les fichiers de build pour maximiser le cache Docker
COPY --chown=1001:root pom.xml mvnw /project/
COPY --chown=1001:root .mvn /project/.mvn

# Pré-télécharger les dépendances
RUN ./mvnw -B -ntp dependency:go-offline

# Copier ensuite les sources
COPY --chown=1001:root src src

# Build natif (le profil s'active via -Dnative)
RUN ./mvnw -B -ntp package -Dnative -DskipTests

# Runtime stage - minimal image
FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/

# Quarkus produit: target/*-runner
COPY --from=build /project/target/*-runner /work/application

# Entrypoint: injecte les secrets (fichiers) en variables d'env attendues par Quarkus
COPY docker-entrypoint.sh /work/docker-entrypoint.sh
RUN chmod 775 /work /work/application /work/docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/work/docker-entrypoint.sh"]
CMD ["/work/application", "-Dquarkus.http.host=0.0.0.0"]
