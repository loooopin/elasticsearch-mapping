### 说明
#### 支持es版本 6.3.2
#### 开发目的
	省去聚合查询构建SearchRequest时，嵌套的去拼装AggregationBuilder。
	省去解析聚合查询结果时for循环嵌套取值，防止业务代码中耦合了大量的只为取值而写的for循环
### QuickStart
#### 配置注解
```
@EsIndex("realtime_1")
public class EsTest {
    @EsField("warehouse_no")
    @EsComparison(ComparisonEnums.EQ)
    private String warehouseNo;
    @EsField("user_id")
    @EsComparison(ComparisonEnums.EQ)
    private String userId;
    @EsField("user_name")
    @EsComparison(ComparisonEnums.EQ)
    private String userName;
    @EsField(value = "user_id", onlyCompare = true)
    @EsComparison(ComparisonEnums.IN)
    private List<String> userIdList;
    @EsField(value = "create_time", onlyCompare = true)
    @EsComparison(ComparisonEnums.GTE)
    private Integer beginTime;
    @EsField(value = "create_time", onlyCompare = true)
    @EsComparison(ComparisonEnums.LT)
    private Integer endTime;

    @EsField("qty_1")
    private Integer qty1;
    @EsField("qty_2")
    private Integer qty2;

	...getter,setter
}
```
#### 查询
```
    public void t4() throws IOException, IllegalAccessException {
        //此处应更换到全局配置
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
        EsSearchHelperFacade searchHelperFacade=new RestHighLevelClientHelper();
        searchHelperFacade.setRestClient(restHighLevelClient);

        //查询参数
        EsTest realtime = new EsTest();
        realtime.setBeginTime(100);
        realtime.setEndTime(101);
        realtime.setUserIdList(new ArrayList() {{
            add("userA");
            add("userB");
        }});
        realtime.setWarehouseNo("warehouseA");

        EsRequestBuilder esRequestBuilder = searchHelperFacade.requestBuilder(realtime).groupBy("userName", "warehouseNo", "userId")
                .aggregation("qty1", AggregateEnums.SUM)
                .aggregation("qty2", AggregateEnums.SUM)
                //非聚合的字段也查询
                .setSearchOtherField(true)
                ;

        //查询
        LinkedList<Map<String, Object>> search = searchHelperFacade.search(esRequestBuilder, EsTest.class);
        System.out.println(JSON.toJSONString(search));
        System.out.println(JSON.parseArray(JSON.toJSONString(search),EsTest.class));
    }
    
    //单个实体对应多个索引时，不使用EsIndex注解，直接在EsRequestBuilder中setEsIndex
    public void t4() throws IOException, IllegalAccessException {
	......
        EsRequestBuilder esRequestBuilder = searchHelperFacade.requestBuilder(realtime).groupBy("userName", "warehouseNo", "userId")
                .aggregation("qty1", AggregateEnums.SUM)
                .aggregation("qty2", AggregateEnums.SUM)
		.setEsIndex("realtime_444")
                //非聚合的字段也查询
                .setSearchOtherField(true)
                ;
	......
    }
```
#### 集成spring
```
@Configuration
public class EsSearchHelperConfig {
    @Bean
    public EsSearchHelperFacade esSearchHelperFacade(){
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
        EsSearchHelperFacade searchHelperFacade=new RestHighLevelClientHelper();
        searchHelperFacade.setRestClient(restHighLevelClient);
        return searchHelperFacade;
    }
}
```
### Q&A
#### 查询时报错 Elasticsearch exception [type=parsing_exception, reason=Unknown key for a VALUE_BOOLEAN in [seq_no_primary_term].]
或报一些其他的es相关的错误。大概率是因为org.elasticsearch:elasticsearch的版本问题。请先确认该依赖以及es其他相关依赖包的版本号是否为6.3.2。
如果你的项目是springboot项目。maven中parent是spring-boot-starter-parent，那么就应该指定elasticsearch.version为6.3.2
#### 查询结果中的某些字段名与实体成员变量的字段对应不上
检查EsField注解是否同一个value在同一个实体中配置了多次。如：
```
    @EsField("column_a")
    @EsComparison(ComparisonEnums.EQ)
    private Integer columnA;
    @EsField("column_a")
    @EsComparison(ComparisonEnums.GTE)
    private Integer beforeColumnA;
    @EsField("column_a")
    @EsComparison(ComparisonEnums.IN)
    private List<Integer> columnAList;
```
应该保证同value的EsField,只应有一个注解的onlyCompare为false
```
    @EsField("column_a")
    @EsComparison(ComparisonEnums.EQ)
    private Integer columnA;
    @EsField(value = "column_a",onlyCompare = true)
    @EsComparison(ComparisonEnums.GTE)
    private Integer beforeColumnA;
    @EsField(value = "column_a",onlyCompare = true)
    @EsComparison(ComparisonEnums.IN)
    private List<Integer> columnAList;
```
### 可能出现的问题
	解析查询结果时用的是递归，且通过流对HashMap进行了深拷贝。所以在groupBy的字段过多，且结果集的数据量大时，可能会有性能问题。
