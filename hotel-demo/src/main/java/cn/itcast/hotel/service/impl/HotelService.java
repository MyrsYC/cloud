package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;


@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("hotel");
            //2.准备DSL
            //2.1 query
            //构建BooleanQuery
            buildBasicQuery(params, request);

            //2.2 分页
            int page=params.getPage();
            int size=params.getSize();
            request.source().from((page-1)*size).size(size);

            //2.3 排序
            String location =params.getLocation();
            if(location!=null&&!location.isEmpty()){
                request.source().sort(SortBuilders
                        .geoDistanceSort("location",new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                );
            }

            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析结果
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            //1.准备Request
            SearchRequest request = new SearchRequest("hotel");
            //2.准备DSL
            //2.1 query
            buildBasicQuery(params, request);
            //2.2设置size
            request.source().size(0);
            //2.2聚合
            buildAggregation(request);

            //3.发送请求
            SearchResponse response=client.search(request, RequestOptions.DEFAULT);

            //4.解析结果
            Map<String,List<String>>result =new HashMap<>();
            Aggregations aggregations = response.getAggregations();
            //4.1 根据品牌名称，获取品牌结果
            List<String> brandList = getAggByName(aggregations,"brandAgg");
            result.put("品牌",brandList);
            //4.2 根据城市名称，获取品牌结果
            List<String> cityList = getAggByName(aggregations,"cityAgg");
            result.put("城市",cityList);
            //4.3 根据星级，获取星级结果
            List<String> starList = getAggByName(aggregations,"starAgg");
            result.put("星级",starList);

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuggestion(String prefix) {
        try {
            //1. 准备Request
            SearchRequest request = new SearchRequest("hotel");
            //2.准备DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions", SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));
            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析响应
            Suggest suggest = response.getSuggest();
            //4.1根据名称获取补全结果
            CompletionSuggestion suggestion= suggest.getSuggestion("suggestions");
            //4.2 获取options
            List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();

            //4.3 遍历
            List<String> result=new ArrayList<>();
            for(CompletionSuggestion.Entry.Option option: options){
                //4.4 获取一个option中的text，即补全的词条
                String text = option.getText().string();
                result.add(text);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            //1.准备Request
            DeleteRequest request= new DeleteRequest("hotel", id.toString());
            //2.发送请求
            client.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
            //0. 准备酒店数据
            Hotel hotel = getById(id);
            //转化为文档类型
            HotelDoc hotelDoc = new HotelDoc(hotel);
            //1. 准备Request对象
            IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
            //2. 准备Json文档
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            //3. 发送请求
            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<String> getAggByName(Aggregations aggregations,String aggName) {
        //4.1 根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        //4.2 获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        //4.3 遍历
        List<String> brandList=new ArrayList<>();
        for(Terms.Bucket bucket: buckets){
            //4.4 获取key
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName")
                .size(100)
        );
    }

    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        BoolQueryBuilder boolQuery =QueryBuilders.boolQuery();
        //关键字搜索
        String key= params.getKey();
        if (key==null|| key.isEmpty()){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else{
            boolQuery.must(QueryBuilders.matchQuery("all",key));
        }
        //城市条件
        String city= params.getCity();
        if (city!=null&&!city.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("city",city));
        }
        //品牌条件
        String brand= params.getBrand();
        if (brand!=null&&!brand.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("brand",brand));
        }
        //星级条件
        String starName= params.getStarName();
        if (starName!=null&&!starName.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("starName",starName));
        }
        //价格范围
        Integer minPrice = params.getMinPrice();
        Integer maxPrice = params.getMaxPrice();
        if (minPrice!=null&&maxPrice!=null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(maxPrice).gte(minPrice));
        }

        //算分控制
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        //原始查询，相关性算分查询
                        boolQuery,
                        //function score的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                //其中一个的function score元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        //过滤条件
                                        QueryBuilders.termQuery("isAD",true),
                                        //算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        request.source().query(functionScoreQuery);
    }

    private PageResult handleResponse(SearchResponse response) {
        //4 解析响应
        SearchHits searchHits = response.getHits();
        //4.1获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到" + total + "条数据");
        //4.2获取文档数组
        SearchHit[] hits = searchHits.getHits();
        //4.3遍历
        List<HotelDoc> hotels=new ArrayList<>();
        for (SearchHit hit : hits) {
            //获取文档
            String json = hit.getSourceAsString();
            //反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //获取排序值
            Object[] sortValues = hit.getSortValues();
            if(sortValues.length>0){
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }

            hotels.add(hotelDoc);
        }
        //4.4 封装返回
        return new PageResult(total,hotels);
    }
}
