package io.github.loooopin.api;

import io.github.loooopin.support.EsRequestBuilder;
import io.github.loooopin.support.EsResponseResolver;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: huxiaodong24
 * Date: 2021/10/13
 * Time: 18:20
 * Description: 查询入口类
 */
public abstract class EsSearchHelperFacade<T extends Closeable> {
    protected T restClient;

    public void setRestClient(T restClient) {
        this.restClient = restClient;
    }

    public abstract LinkedList<Map<String,Object>> search(EsRequestBuilder requestBuilder, Class responseClass) throws IOException, IllegalAccessException;

    public EsRequestBuilder requestBuilder(Object request) {
        return EsRequestBuilder.builder(request);
    }

    public EsRequestBuilder requestBuilder(Class _class) {
        return EsRequestBuilder.builder(_class);
    }

    public EsResponseResolver responseResolver(EsRequestBuilder requestBuilder, Class responseClass){
        return EsResponseResolver.resolver(responseClass
                , new LinkedList(requestBuilder.getGroupBuilders().keySet())
                , requestBuilder.isAggregationQuery());
    }
}
