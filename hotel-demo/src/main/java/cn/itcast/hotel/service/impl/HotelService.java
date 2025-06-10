package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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

            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析结果
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        request.source().query(boolQuery);
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

            hotels.add(hotelDoc);
        }
        //4.4 封装返回
        return new PageResult(total,hotels);
    }
}
