FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY caries-common/pom.xml caries-common/pom.xml
COPY caries-framework/pom.xml caries-framework/pom.xml
COPY caries-system/pom.xml caries-system/pom.xml
COPY caries-integration/pom.xml caries-integration/pom.xml
COPY caries-patient/pom.xml caries-patient/pom.xml
COPY caries-image/pom.xml caries-image/pom.xml
COPY caries-analysis/pom.xml caries-analysis/pom.xml
COPY caries-report/pom.xml caries-report/pom.xml
COPY caries-followup/pom.xml caries-followup/pom.xml
COPY caries-dashboard/pom.xml caries-dashboard/pom.xml
COPY caries-boot/pom.xml caries-boot/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -q -DskipTests -pl caries-boot -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /workspace/caries-boot/target/caries-boot-0.1.0-SNAPSHOT.jar /app/caries-boot.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/caries-boot.jar"]