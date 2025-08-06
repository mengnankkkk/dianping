package com.mengnankk.service.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mengnankk.dto.SearchResult;
import com.mengnankk.entity.Shop;
import jodd.time.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.search.OpenPointInTimeRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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

    /**
     * 精确匹配
     * @param keywords
     * @return
     * @throws IOException
     */

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

    /**
     * 模糊匹配+精确匹配
     * @param searchText
     * @param pageNum
     * @param pageSize
     * @return
     * @throws IOException
     */

    public List<Shop> searchPersonWithBoolQuery(String searchText, int pageNum, int pageSize) throws IOException {
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
            sourceBuilder.from((pageNum - 1) * pageSize).size(pageSize);
            //高亮name
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("name").preTags(HIGHLIGHT_PRE_TAG).postTags(HIGHLIGHT_POST_TAG);
            sourceBuilder.highlighter(highlightBuilder);


            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            long totalHits = searchResponse.getHits().getTotalHits().value;

            return convertSearchHitsToShops(searchResponse);
        } catch (IOException e) {
            log.error("查询错误");
            return new ArrayList<>();
        }
    }

    /**
     *
     * @param queryString
     * @return
     * @throws IOException
     */

    public List<Shop> searchPersonWithLuceneQuery(String queryString) throws IOException {
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            QueryBuilder queryBuilders = QueryBuilders.queryStringQuery(queryString);

            sourceBuilder.query(queryBuilders);
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
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
            if (hit.getHighlightFields() != null) {
                HighlightField nameField = hit.getHighlightFields().get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.fragments();
                    String highlightedName = fragments[0].toString();
                    shop.setName(highlightedName);
                }
            }

            shops.add(shop);
        }
        return shops;
    }

    /**
     * 监控
     */
    @PostConstruct
    public void monitorClusterHealth() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            try {
                ClusterHealthRequest request = new ClusterHealthRequest();
                ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);
                ClusterHealthStatus status = response.getStatus();

                if (status == ClusterHealthStatus.RED || status == ClusterHealthStatus.YELLOW) {
                    log.error("Elasticsearch 集群状态异常: {}", status);
                    sendAlert("Elasticsearch 集群状态异常: " + status);
                } else {
                    log.info("Elasticsearch 集群状态正常: {}", status);
                }
            } catch (IOException e) {
                log.error("检查集群健康状态失败", e);
            }
        }, 0, MONITOR_INTERVAL, TimeUnit.SECONDS);
    }

    private void sendAlert(String message) {
        // TODO: 实现短信/邮件通知
        System.out.println("【告警】" + message);
    }

    /**
     * 分页查询优化
     * @param keywords
     * @param pageSize
     * @param searchAfterSortValues
     * @param sortField
     * @param sortOrder
     * @return
     * @throws IOException
     */
    public SearchResult<Shop> searchShopsAfter(String keywords, int pageSize, Object[] searchAfterSortValues, String sortField, String sortOrder) throws IOException {

        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", keywords);
            boolQueryBuilder.must(matchQueryBuilder);

            sourceBuilder.query(boolQueryBuilder);
            sourceBuilder.size(pageSize);

            SortOrder order = sortOrder.equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(new FieldSortBuilder(sortField).order(order));

            if (searchAfterSortValues != null) {
                sourceBuilder.searchAfter(searchAfterSortValues);
            }

            searchRequest.source(sourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            List<Shop> shops = convertSearchHitsToShops(searchResponse);

            Object[] nextSearchAfter = null;
            if (shops.size() > 0) {
                SearchHit lasthit = searchResponse.getHits().getHits()[shops.size() - 1];
                nextSearchAfter = lasthit.getSortValues();
            }
            SearchResult<Shop> result = SearchResult.<Shop>builder()
                    .results(shops)
                    .nextSearchAfter(nextSearchAfter)
                    .total(shops.size())
                    .build();
            return result;
        } catch (IOException e) {
            log.info("使用 search_after 搜索店铺时发生IO异常", e);
            return null;
        }

    }

    /**
     * scrollId搜索
     * @param keywords
     * @param pageSize
     * @param scrollId
     * @param scrollTime
     * @return
     * @throws IOException
     */
    public List<Shop> searchShopsByScroll(String keywords, int pageSize, String scrollId, String scrollTime) throws IOException{
        if (scrollId==null){
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", keywords);
            boolQueryBuilder.must(matchQueryBuilder);

            sourceBuilder.query(boolQueryBuilder);
            sourceBuilder.size(pageSize);
            searchRequest.source(sourceBuilder);
            searchRequest.scroll(TimeValue.parseTimeValue(scrollTime,null,"scroll"));//设置scroll时间

            SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
            return convertSearchHitsToShops(searchResponse);
        }else {
            //后续请求
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.parseTimeValue(scrollTime, null, "scroll"));
            SearchResponse searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            return convertSearchHitsToShops(searchResponse);
        }

    }
}