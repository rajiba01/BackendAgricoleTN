FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn

RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/AI_Powered_TunisianEconomic_Intelligence_System-1.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080

CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
