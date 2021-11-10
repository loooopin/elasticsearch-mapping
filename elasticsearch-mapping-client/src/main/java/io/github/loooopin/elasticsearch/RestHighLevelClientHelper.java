package io.github.loooopin.elasticsearch;

import io.github.loooopin.elasticsearch.api.EsSearchHelperFacade;
import io.github.loooopin.elasticsearch.entity.EsResponse;
import io.github.loooopin.elasticsearch.support.DefaultEsRequestBuilder;
import io.github.loooopin.elasticsearch.support.DefaultEsResponseResolver;
import io.github.loooopin.elasticsearch.support.exceptions.EsSearchException;
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
    public EsResponse search(DefaultEsRequestBuilder requestBuilder) {
        SearchRequest searchRequest = requestBuilder.build();
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restClient.search(searchRequest);
        } catch (IOException e) {
            throw new EsSearchException("查询时发生异常", e);
        }
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder);
        return resolver.resolve(searchResponse);
    }

    @Override
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, int from, int size) {
        SearchRequest searchRequest = requestBuilder.build(from, size);
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restClient.search(searchRequest);
        } catch (IOException e) {
            throw new EsSearchException("查询时发生异常", e);
        }
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder);
        return resolver.resolve(searchResponse);
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Object request) {
        return new DefaultEsRequestBuilder().setRequest(request).loadContext(request.getClass(), request.getClass());
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Class requestClass) {
        return new DefaultEsRequestBuilder().loadContext(requestClass, requestClass);
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Object request, Class responseClass) {
        return new DefaultEsRequestBuilder().setRequest(request).loadContext(request.getClass(), responseClass);
    }

    @Override
    public DefaultEsRequestBuilder requestBuilder(Class requestClass, Class responseClass) {
        return new DefaultEsRequestBuilder().loadContext(requestClass, responseClass);
    }

    @Override
    public DefaultEsResponseResolver responseResolver(DefaultEsRequestBuilder requestBuilder) {
        return new DefaultEsResponseResolver(requestBuilder.getGroupFieldChain(), requestBuilder.isAggregationQuery());
    }

    @Override
    @Deprecated
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, Class responseClass) {
        SearchRequest searchRequest = requestBuilder.build();
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restClient.search(searchRequest);
        } catch (IOException e) {
            throw new EsSearchException("查询时发生异常", e);
        }
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder, responseClass);
        return resolver.resolve(searchResponse);
    }

    @Override
    @Deprecated
    public EsResponse search(DefaultEsRequestBuilder requestBuilder, Class responseClass, int from, int size) {
        SearchRequest searchRequest = requestBuilder.build(from, size);
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restClient.search(searchRequest);
        } catch (IOException e) {
            throw new EsSearchException("查询时发生异常", e);
        }
        DefaultEsResponseResolver resolver = this.responseResolver(requestBuilder, responseClass);
        return resolver.resolve(searchResponse);
    }

    @Override
    @Deprecated
    public DefaultEsResponseResolver responseResolver(DefaultEsRequestBuilder requestBuilder, Class responseClass) {
        return new DefaultEsResponseResolver(responseClass, requestBuilder.getGroupFieldChain(), requestBuilder.isAggregationQuery()).loadContext();
    }
}
