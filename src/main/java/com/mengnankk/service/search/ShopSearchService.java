package com.mengnankk.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mengnankk.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShopSearchService {
    private static final String INDEX_NAME = "shop";
    private final RestHighLevelClient client;
    private final ObjectMapper mapper;
    @Value("${elasticsearch.highlight.preTag:<em>}")
    private String HIGHLIGHT_PRE_TAG;
    @Value("${elasticsearch.highlight.postTag:</em>}")
    private String HIGHLIGHT_POST_TAG;
    @Value("${elasticsearch.monitor.interval:60}")
    private int MONITOR_INTERVAL;

    public ShopSearchService(RestHighLevelClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public List<Shop> searchShops(String keywords) throws IOException {
        SearchResponse searchResponse = null;
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //精确匹配
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", keywords);
            boolQueryBuilder.must(matchQueryBuilder);

            sourceBuilder.query(boolQueryBuilder);
            searchRequest.source(sourceBuilder);

            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return convertSearchHitsToShops(searchResponse);
        } catch (IOException e) {
            log.error("查询错误");
            return new ArrayList<>();
        }
    }

    public List<Shop> searchPersonWithBoolQuery(String searchText,int pageNum, int pageSize) throws IOException {
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //模糊匹配+精确匹配
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("fullName", searchText);
            boolQueryBuilder.should(matchQueryBuilder);
            FuzzyQueryBuilder fuzzyQueryBuilder = QueryBuilders.fuzzyQuery("fullName", searchText).fuzziness(Fuzziness.AUTO);
            boolQueryBuilder.should(fuzzyQueryBuilder);

            sourceBuilder.query(boolQueryBuilder);
            //分页
            sourceBuilder.from((pageNum-1)*pageSize).size(pageSize);
            //高亮name
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("name").preTags(HIGHLIGHT_PRE_TAG).postTags(HIGHLIGHT_POST_TAG);
            sourceBuilder.highlighter(highlightBuilder);


            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            long totalHits = searchResponse.getHits().getTotalHits().value;

            return convertSearchHitsToShops(searchResponse);
        } catch (IOException e) {
            log.error("查询错误");
            return new ArrayList<>();
        }
    }
    public List<Shop> searchPersonWithLuceneQuery(String queryString) throws IOException{
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            QueryBuilder queryBuilders =QueryBuilders.queryStringQuery(queryString);

            sourceBuilder.query(queryBuilders);
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            return convertSearchHitsToShops(searchResponse);
        } catch (IOException e) {
            log.error("查询错误");
            return new ArrayList<>();
        }

    }
    private List<Shop> convertSearchHitsToShops(SearchResponse searchResponse) {
        List<Shop> shops = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            Shop shop = mapper.convertValue(sourceAsMap, Shop.class);
            //高亮显示
            if (hit.getHighlightFields()!=null){
                HighlightField nameField = hit.getHighlightFields().get("name");
                if (nameField!=null){
                    Text[] fragments = nameField.fragments();
                    String highlightedName = fragments[0].toString();
                    shop.setName(highlightedName);
                }
            }

            shops.add(shop);
        }
        return shops;
    }
    @PostConstruct
    public void monitorClusterHealth(){
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(()->{
            try {
                ClusterHealthRequest request = new ClusterHealthRequest();
                ClusterHealthResponse response = client.cluster().health(request,RequestOptions.DEFAULT);
                ClusterHealthStatus status  =response.getStatus();

                if (status==ClusterHealthStatus.RED||status==ClusterHealthStatus.YELLOW){
                    log.error("Elasticsearch 集群状态异常: {}", status);
                    sendAlert("Elasticsearch 集群状态异常: " + status);
                }else {
                    log.info("Elasticsearch 集群状态正常: {}", status);
                }
            }catch (IOException e){
                log.error("检查集群健康状态失败", e);
            }
        },0,MONITOR_INTERVAL, TimeUnit.SECONDS);
    }
    private void sendAlert(String message) {
        // TODO: 实现短信/邮件通知
        System.out.println("【告警】" + message);
    }
}