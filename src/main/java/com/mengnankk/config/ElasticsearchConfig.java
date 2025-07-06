package com.mengnankk.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticsearchConfig {
    private static final String HOST = "localhost"; // Elasticsearch 服务地址
    private static final int PORT = 9200; // Elasticsearch 端口
    private static final String SCHEME = "http"; // 协议

    public static RestHighLevelClient getClient(){
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(HOST,PORT,SCHEME)));

    }
    public static void close(RestHighLevelClient client){
        try {
            client.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
