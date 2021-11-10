package io.github.loooopin.elasticsearch;

import io.github.loooopin.elasticsearch.api.EsSearchHelperFacade;
import io.github.loooopin.elasticsearch.entity.EsResponse;
import io.github.loooopin.elasticsearch.support.DefaultEsRequestBuilder;
import io.github.loooopin.elasticsearch.support.DefaultEsResponseResolver;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

/**
 * User: loooopin
 * Date: 2021/10/14
 * Time: 20:10
 * Description: 高版本查询
 */
public class RestHighLevelClientHelper extends EsSearchHelperFacade<RestHighLevelClient, DefaultEsRequestBuilder, DefaultEsResponseResolver> {
    @Override
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, Class responseClass) throws IOException {
        SearchRequest searchRequest = requestBuilder.build();
        SearchResponse searchResponse = this.restClient.search(searchRequest);
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder, responseClass);
        return resolver.resolve(searchResponse);
    }

    @Override
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, Class responseClass, int from, int size) throws IOException {
        SearchRequest searchRequest = requestBuilder.build(from, size);
        SearchResponse searchResponse = this.restClient.search(searchRequest);
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder, responseClass);
        return resolver.resolve(searchResponse);
    }

    @Override
    public EsResponse search(DefaultEsRequestBuilder requestBuilder) throws IOException {
        return this.search(requestBuilder, requestBuilder.getRequestClass());
    }

    @Override
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, int from, int size) throws IOException {
        return this.search(requestBuilder, requestBuilder.getRequestClass(), from, size);
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Object request) throws IllegalAccessException {
        return new DefaultEsRequestBuilder().setRequest(request);
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Class _class) throws IllegalAccessException {
        return new DefaultEsRequestBuilder().loadContext(_class);
    }

    @Override
    public DefaultEsResponseResolver responseResolver(DefaultEsRequestBuilder esRequestBuilder, Class responseClass) {
        return new DefaultEsResponseResolver(responseClass, esRequestBuilder.getGroupFieldChain(), esRequestBuilder.isAggregationQuery()).loadContext();
    }
}
