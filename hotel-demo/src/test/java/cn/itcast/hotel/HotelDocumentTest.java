package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;
import java.util.List;

@SpringBootTest
public class HotelDocumentTest {
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;
    @Test
    void testAddDocument() throws IOException {
        //根据id查询数据
        Hotel hotel = hotelService.getById(61083L);
        //转化为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);
        //1. 准备Request对象
        IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
        //2. 准备Json文档
        request.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //3. 发送请求
        client.index(request,RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocumentById() throws IOException {
        //1.准备Request
        GetRequest request = new GetRequest("hotel", "61083");
        //2.发送请求，得到响应
        GetResponse response= client.get(request, RequestOptions.DEFAULT);
        //3.解析响应结果
        String json = response.getSourceAsString();
        //JSON转为对象
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }


    @Test
    void testUpdateDocumentById() throws IOException {
        //1.准备Request
        UpdateRequest request = new UpdateRequest("hotel", "61083");
        //2.准备参数
        request.doc(
                "price","952",
                "starName","一钻"
        );
        //3.发送请求
        client.update(request,RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocumentById() throws IOException {
        //1.准备Request
        DeleteRequest request= new DeleteRequest("hotel", "61083");
        //2.发送请求
        client.delete(request,RequestOptions.DEFAULT);
    }

    @Test
    void testBulkRequest() throws IOException {
        //批量查询
        List<Hotel> hotels = hotelService.list();
        //1.创建Request
        BulkRequest request = new BulkRequest();
        //2.准备参数，添加多个新增的Request
        for(Hotel hotel:hotels){
            //转换为文档类型
            HotelDoc hotelDoc = new HotelDoc(hotel);
            //创建新增文档的Request对象
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        }
        //3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);

    }


    @BeforeEach
    void setUp(){
        this.client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.137.28:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }


}
