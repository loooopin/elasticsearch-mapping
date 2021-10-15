package io.github.loooopin;

import com.es.mapping.api.EsSearchHelperFacade;
import com.es.mapping.support.EsRequestBuilder;
import com.es.mapping.support.EsResponseResolver;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: huxiaodong24
 * Date: 2021/10/14
 * Time: 20:10
 * Description: 高版本查询
 */
public class RestHighLevelClientHelper extends EsSearchHelperFacade<RestHighLevelClient> {
    @Override
    public LinkedList<Map<String, Object>> search(EsRequestBuilder requestBuilder, Class responseClass) throws IOException, IllegalAccessException {
        requestBuilder.build();
        SearchRequest searchRequest = new SearchRequest(requestBuilder.getIndex());
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(requestBuilder.getFilter());
        if (requestBuilder.hasAggregations()) {
            sourceBuilder.aggregation(requestBuilder.getAggregations());
        }
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = this.restClient.search(searchRequest);
        EsResponseResolver resolver = this.responseResolver(requestBuilder, responseClass);
        return resolver.resolve(searchResponse);
    }
}
