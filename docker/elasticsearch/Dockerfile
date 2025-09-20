# 基于官方 Elasticsearch 8.11.0 镜像
FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# 切换到 elasticsearch 用户
USER elasticsearch

# 安装 IK 分词器插件 - 使用正确的下载链接
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.11.0

# 保持以 elasticsearch 用户运行（不切换回root）

# 暴露端口
EXPOSE 9200 9300