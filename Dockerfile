# 多阶段构建：前端构建
FROM node:18-alpine AS frontend-builder
WORKDIR /app
COPY vue/package.json vue/package-lock.json ./
RUN npm install
COPY vue/ .
RUN npm run build

# 多阶段构建：后端构建
FROM maven:3.8.6-openjdk-17 AS backend-builder
WORKDIR /app
COPY springboot/pom.xml .
RUN mvn dependency:go-offline
COPY springboot/src/ ./src/
COPY --from=frontend-builder /app/dist/ ./src/main/resources/static/
RUN mvn package -DskipTests

# 生产环境镜像
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=backend-builder /app/target/smart-agriculture-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9094
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]