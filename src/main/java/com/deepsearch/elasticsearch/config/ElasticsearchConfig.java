package com.deepsearch.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.scheme:http}")
    private String scheme;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${elasticsearch.socket-timeout:30000}")
    private int socketTimeout;

    @Value("${elasticsearch.max-connections:100}")
    private int maxConnections;

    @Value("${elasticsearch.max-connections-per-route:10}")
    private int maxConnectionsPerRoute;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            RestClientBuilder builder = RestClient.builder(
                new HttpHost(host, port, scheme)
            );

            // Configure HTTP client settings
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder
                    .setMaxConnTotal(maxConnections)
                    .setMaxConnPerRoute(maxConnectionsPerRoute);

                // Add authentication if provided
                if (!username.isEmpty() && !password.isEmpty()) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                return httpClientBuilder;
            });

            // Configure request timeouts
            builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
            );

            RestClient restClient = builder.build();
            ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
            );

            ElasticsearchClient client = new ElasticsearchClient(transport);

            logger.info("Elasticsearch client configured successfully - Host: {}:{}, Scheme: {}",
                host, port, scheme);

            return client;
        } catch (Exception e) {
            logger.error("Failed to configure Elasticsearch client", e);
            throw new RuntimeException("Elasticsearch client configuration failed", e);
        }
    }
}