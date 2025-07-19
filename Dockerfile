FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 复制项目文件
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ /build/src/
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# 创建数据库目录
RUN mkdir -p /app/db

# 环境变量，可以在运行容器时覆盖
ENV CLASH_API_BASE_URL=http://host.docker.internal:9090
ENV CLASH_API_SECRET=
ENV SERVER_PORT=7899
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/db/clash_auto_change.db

# 自动切换配置环境变量
ENV CLASH_AUTO_CHANGE_TEST_URL=https://www.gstatic.com/generate_204
ENV CLASH_AUTO_CHANGE_CHECK_INTERVAL=5000

# 暴露端口
EXPOSE ${SERVER_PORT}

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"] 