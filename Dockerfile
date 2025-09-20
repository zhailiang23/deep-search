# 使用OpenJDK 17作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制Gradle构建文件
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

# 下载依赖项（利用Docker层缓存）
RUN ./gradlew dependencies --no-daemon

# 复制源代码
COPY src src

# 构建应用程序
RUN ./gradlew build --no-daemon

# 暴露端口
EXPOSE 8080

# 设置健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 运行应用程序
CMD ["java", "-jar", "build/libs/deep-search-1.0.0.jar"]