* [说明](#说明)
  * [支持es版本 6\.3\.2](#支持es版本-632)
  * [开发目的](#开发目的)
* [QuickStart](#quickstart)
  * [配置注解](#配置注解)
  * [查询](#查询)
  * [集成spring](#集成spring)
* [Q&amp;A](#qa)
  * [我的实体类都是继承于父类的，父类无法加注解怎么办](#我的实体类都是继承于父类的父类无法加注解怎么办)
  * [我想从配置文件中读取es索引名](#我想从配置文件中读取es索引名)
  * [应该怎么使用search\_after](#应该怎么使用search_after)
  * [我想实现类似于sql中聚合字段的别名（SUM(qty) AS totalQty）。应该怎么做](#我想实现类似于sql中聚合字段的别名sumqty-as-totalqty应该怎么做)
  * [我的查询参数与返回结果不是同一个实体类应该怎么办](#我的查询参数与返回结果不是同一个实体类应该怎么办)
  * [查询时报错 Elasticsearch exception [type=parsing\_exception, reason=Unknown key for a VALUE\_BOOLEAN in [seq\_no\_primary\_term]\.]](#查询时报错-elasticsearch-exception-typeparsing_exception-reasonunknown-key-for-a-value_boolean-in-seq_no_primary_term)
  * [查询结果中的某些字段名与实体成员变量的字段对应不上](#查询结果中的某些字段名与实体成员变量的字段对应不上)
* [可能出现的问题](#可能出现的问题)

### 说明
#### 版本相关
	支持的es版本:6.3.2,理论上支持6.3.2-7.0.0
	使用的org.elasticsearch版本：6.3.2
	使用的org.elasticsearch.client版本：rest-high-level-client:6.3.2
#### 开发目的
	减少es查询语句的学习成本。
	省去聚合查询构建SearchRequest时，嵌套的去拼装AggregationBuilder。
	省去解析聚合查询结果时for循环嵌套取值，防止业务代码中耦合了大量的只为取值而写的for循环。
### QuickStart
#### 引入对应版本的client包
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
    public void t4() {
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

        EsRequestBuilder esRequestBuilder = searchHelperFacade.requestBuilder(realtime)
                .groupBy("userName", "warehouseNo", "userId")
                .aggregation("qty1", AggregateEnums.SUM)
                .aggregation("qty2", AggregateEnums.SUM)
                //非聚合的字段也查询
                .setSearchOtherField(true)
                ;

        //查询
        LinkedList<Map<String, Object>> search = searchHelperFacade.search(esRequestBuilder);
        System.out.println(JSON.toJSONString(search));
        System.out.println(JSON.parseArray(JSON.toJSONString(search),EsTest.class));
    }
    
    //单个实体对应多个索引时，不使用EsIndex注解，直接在EsRequestBuilder中setEsIndex
    public void t4() {
	......
        EsRequestBuilder esRequestBuilder = searchHelperFacade.requestBuilder(realtime)
                .groupBy("userName", "warehouseNo", "userId")
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
#### 我的实体类都是继承于父类的，父类无法加注解怎么办
调用EsBeanContext#addContext方法，手动将相关映射配置到EsBeanContext中。该操作全局只需执行一次，不需要每次查询都add一次
#### 我想从配置文件中读取es索引名
使用@EsIndex{"@{key}"}
#### 应该怎么使用search_after
使用分页查询，每次分页会返回lastHitSortValues（本次查询的最后一个sortValues）。调用EsRequestBuilder#setSortValues传入该值即可。
注：1)聚合查询不支持分页;2)没有设置orderBy的话，返回的lastHitSortValues将为空
#### 我想实现类似于sql中聚合字段的别名（SUM(qty) AS totalQty）。应该怎么做
```
esRequestBuilder.aggregation("qty",AggregateEnums.SUM,"totalQty")
```
aggregation方法中第一个参数为java实体的字段名(如果没有该字段，则应调用EsBeanContext#addContext手动配置)
第二个参数为聚合函数
第三个参数为想要映射到的字段

或
```
Entity.java
......
@EsField("skuQty")
private Integer totalQty;
......

Context
......
esRequestBuilder.aggregation("qty",AggregateEnums.SUM)
......
```
注意：使用EsField建立成员变量与es列名的对应关系时，必须是一对一的。不能一对多或多对一或多对多。如果你的Entity.java中还有一个qty字段，那么建议使用第一种方法
#### 我的查询参数与返回结果不是同一个实体类应该怎么办
```
DefaultEsRequestBuilder requestBuilder = (DefaultEsRequestBuilder)esSearchHelperFacade.requestBuilder(param,responseClass);
```
在获得requestBuilder时设置返回值的类型，如果不设置，则默认使用查询参数的类型
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
