FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Spécifier le nom exact pour ignorer le fichier 'original-*.jar'
COPY --from=build /app/target/AI_Powered_TunisianEconomic_Intelligence_System-1.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8085
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]