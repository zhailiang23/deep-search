# 使用OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制Maven构建文件
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./

# 下载依赖项（利用Docker层缓存）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 构建应用程序
RUN ./mvnw package -DskipTests -B

# 暴露端口
EXPOSE 8080

# 设置健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 运行应用程序
CMD ["java", "-jar", "target/deep-search-1.0.0.jar"]