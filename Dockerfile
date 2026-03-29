# Build stage - Maven (descarga dependencias y compila TODO)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias (cache)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar código fuente y compilar (incluye dependencias en el WAR)
COPY src ./src
RUN mvn package -DskipTests -q

# Imagen final - solo Tomcat, pero con WAR completo
FROM tomcat:10.1-jre17-temurin

# Limpiar apps por defecto
RUN rm -rf /usr/local/tomcat/webapps/*

# Copiar WAR generado (con TODAS las dependencias incluidas)
COPY --from=build /app/target/password-manager.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]