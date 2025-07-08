FROM openjdk:17-slim as build
WORKDIR /workspace/app

# 复制 Maven 配置文件
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# 构建应用程序
RUN chmod +x ./mvnw
RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# 运行阶段
FROM openjdk:17-slim
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency

# 复制项目依赖
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# 数据卷，用于持久化SQLite数据库
VOLUME /app/db

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
ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.github.clashautochange.ClashAutoChangeApplication"] 