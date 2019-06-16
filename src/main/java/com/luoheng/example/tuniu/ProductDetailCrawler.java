package com.luoheng.example.tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ProductDetailCrawler extends Crawler {
    private static final String FROM_TOUR_QUEUE="list_tuniu_tour_product_id";
    private static final String FROM_PKG_QUEUE="list_tuniu_pkg_product_id";
    private static final String CITY_URL="https://m.tuniu.com/wap-detail/api/group/city";
    private static final String DETAIL_URL="https://m.tuniu.com/wap-detail/api/self/detail/getProductInfo";
    private static final String PRICE_URL="https://m.tuniu.com/wap-detail/api/group/price";
    private static final String CALENDAR_URL="https://m.tuniu.com/wap-detail/api/group/calendar";
    private static final String JOURNEY_URL="https://m.tuniu.com/wap-detail/api/group/journey";
    private static final String TEMPLATE_TOUR_PRODUCT_LINK="http://www.tuniu.com/tour/%s";
    private static final String TEMPLATE_PKG_PRODUCT_LINK="http://www.tuniu.com/package/%s";
    public static final int TYPE_TOUR=0;
    public static final int TYPE_PKG=1;
    private int type;
    private Gson gson;
    private Logger logger= LogManager.getLogger(ProductDetailCrawler.class);
    public ProductDetailCrawler(CrawlerFactory factory,int type) {
        super(factory);
        this.type=type;
    }

    public ProductDetailCrawler(CrawlerFactory factory,String name,int type) {
        super(factory,name);
        this.type=type;
    }

    public ProductDetailCrawler(CrawlerFactory factory,String name, long crawlInterval,int type) {
        super(factory,name,crawlInterval);
        this.type=type;
    }

    @Override
    public void init() {
        super.init();
        gson=new Gson();
    }

    @Override
    public String getTaskData() {
        Jedis jedis=JedisUtil.getResource();
        while(!isOver()){
            String productId;
            if(type==TYPE_TOUR)
                productId=jedis.lpop(FROM_TOUR_QUEUE);
            else
                productId=jedis.lpop(FROM_PKG_QUEUE);
            if(productId!=null)
                return productId;
        }
        return null;
    }

    private Map<String,Integer> getCityInfo(String taskData) throws IOException{
        Map<String,Integer> cityMap=new HashMap<>();
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("productId",taskData);
        params.put("bookCityCode","2500");
        headers.put("User-Agent","Chrome/74.0.3729.169 Mobile");
        headers.put("Referer","https://m.tuniu.com/h5/package/"+taskData);
        Response response=HttpUtil.doGet(CITY_URL,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            JsonArray secondCityList=data.getAsJsonObject("departCities")
                    .getAsJsonObject("allCities").getAsJsonArray("secondCityList");
            for(int i=0;i<secondCityList.size();i++){
                JsonObject item=secondCityList.get(i).getAsJsonObject();
                JsonArray thirdCityList=item.getAsJsonArray("thirdCityList");
                for(int j=0;j<thirdCityList.size();j++){
                    JsonObject object=thirdCityList.get(j).getAsJsonObject();
                    cityMap.put(object.get("bookCityName").getAsString(),object.get("bookCityCode").getAsInt());
                }
            }
            return cityMap;
        }
        else{
            logger.info("failed to request "+response.request().url());
            return null;
        }
    }

    private void getProductBasicInfo(String taskData,JsonObject result) throws IOException {
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("d", String.format("{\"productId\":\"%s\",\"journeyId\":0,\"bookCityCode\":2500}", taskData));
        headers.put("User-Agent", "Chrome/74.0.3729.169 Mobile");
        headers.put("Referer", "https://m.tuniu.com/h5/package/"+taskData);
        Response response=HttpUtil.doGet(DETAIL_URL, params, headers);
        if(response.code()==200) {
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr, JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            result.add("name", data.get("name"));
            if(data.has("journeyList")){
                result.add("journeyList",data.getAsJsonArray("journeyList"));
            }
        } else{
            logger.info("failed to request "+response.request().url());
        }
    }

    private void getProductVendor(Map<String,Integer> cityInfo,String taskData,JsonObject result) throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        JsonArray cityArrayResult=new JsonArray();
        for(Map.Entry<String,Integer> entry:cityInfo.entrySet()){
            String cityName=entry.getKey();
            int cityCode=entry.getValue();
            JsonObject cityResult=new JsonObject();
            cityResult.addProperty("cityName",cityName);
            params.put("productId", taskData);
            params.put("bookCity", cityCode+"");
            Response response=HttpUtil.doGet(PRICE_URL, params, headers);
            if(response.code()==200) {
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr, JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                JsonObject vendorInfo=data.getAsJsonObject("vendorInfo");
                JsonElement companyName=vendorInfo.get("companyName");
                if(companyName.getAsString()==null)
                    cityResult.addProperty("companyName", "null");
                else
                    cityResult.addProperty("companyName", companyName.getAsString());
                cityResult.addProperty("fullName", vendorInfo.get("fullName").getAsString());
                getProductCalendar(taskData,cityCode,cityResult);
                cityArrayResult.add(cityResult);
            } else {
                logger.info("failed to request "+response.request().url());
                return;
            }
        }
        result.add("cityList",cityArrayResult);
    }
    private void getProductCalendar(String taskData,int cityCode,JsonObject result) throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("productId",taskData);
        params.put("bookCityCode",cityCode+"");
        Response response=HttpUtil.doGet(CALENDAR_URL,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            if(!data.has("calendarInfos")){
                logger.info(response.request().url());
                logger.info(responseStr);
            }
            JsonArray calendarInfos=data.getAsJsonArray("calendarInfos");
            JsonArray calendarResult=new JsonArray();
            for(int i=0;i<calendarInfos.size();i++){
                JsonArray calendarDetails=calendarInfos.get(i)
                        .getAsJsonObject().getAsJsonArray("calendarDetails");
                for(int j=0;j<calendarDetails.size();j++){
                    JsonObject object=calendarDetails.get(j).getAsJsonObject();
                    JsonObject objectResult=new JsonObject();
                    objectResult.addProperty("planDate",object.get("planDate").getAsString());
                    objectResult.addProperty("adultPrice",object.get("adultPrice").getAsString());
                    calendarResult.add(objectResult);
                }
            }
            if(result.has("calendar")){
                JsonArray array=result.getAsJsonArray("calendar");
                array.addAll(calendarResult);
            }
            else
                result.add("calendar",calendarResult);
        }
        else{
            logger.info("failed to request "+response.request().url());
        }
    }

    private void getProductJourney(String taskData,JsonObject result) throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("productId",taskData);
        Response response=HttpUtil.doGet(JOURNEY_URL,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            JsonArray journeyDetailList=data.getAsJsonArray("journeyDetailList");
            //journey detail result
            JsonArray journeyListResult=new JsonArray();
            for(int i=0;i<journeyDetailList.size();i++){
                //journey detail result
                JsonArray journeyDetailResult=new JsonArray();
                JsonObject listItem=journeyDetailList.get(i).getAsJsonObject();
                JsonObject journeyFourDetail=listItem.getAsJsonObject("journeyFourDetail");
                JsonArray overview=journeyFourDetail.getAsJsonArray("overview");
                for(int j=0;j<overview.size();j++){
                    JsonObject item=overview.get(j).getAsJsonObject();
                    //journey detail result item
                    JsonObject objectResult=new JsonObject();
                    JsonElement journeyDescription=item.get("journeyDescription");
                    JsonElement scenic=item.get("scenic");
                    objectResult.addProperty("journeyDescription",journeyDescription.getAsString());
                    objectResult.add("scenic",scenic);
                    journeyDetailResult.add(objectResult);
                }
                journeyListResult.add(journeyDetailResult);
            }
            if(result.has("journeyList")){
                JsonArray journeyList=result.getAsJsonArray("journeyList");
                journeyList.addAll(journeyListResult);
            }
            else
                result.add("journeyList",journeyListResult);
        }
        else{
            logger.info("failed to request "+response.request().url());
        }
    }

    //taskData 产品编号
    @Override
    public void crawl(String taskData) {
        JsonObject result=new JsonObject();
        result.addProperty("productId",taskData);
        if(type==TYPE_TOUR){
            result.addProperty("productLink", String.format(TEMPLATE_TOUR_PRODUCT_LINK,taskData));
            result.addProperty("type","tour");
        }
        else{
            result.addProperty("productLink", String.format(TEMPLATE_PKG_PRODUCT_LINK,taskData));
            result.addProperty("type","pkg");
        }
        try{
            Map<String,Integer> cityInfo=getCityInfo(taskData);
            getProductBasicInfo(taskData,result);
            getProductJourney(taskData,result);
            getProductVendor(cityInfo,taskData,result);
            saveData(result);
        }catch(IOException e){
            e.printStackTrace();
        }
        logger.info("end");
    }
    private void saveData(JsonObject object){
        //logger.info(object.toString());
        /*try{
            Connection connection=DBPoolUtil.getConnection();

            PreparedStatement statement=connection.prepareStatement("INSERT INTO" +
                    " TRAVEL_PRODUCT_INFO(`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`,`TA_NAME`," +
                    "`PACKAGE_NAME`,`TRAVEL_PLAN`,`TOUR_PARTY`,`PARYT_PALCE`,`TOUR_DESTN`,`TOUR_DAYS`,`TOUR_PATH`)" +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
            statement.setString(1, ThreadUtil.getUUID());
            statement.setInt(2,6);
            statement.setString(3,object.get("type").getAsString());
            statement.setString(4,object.get("productId").getAsString());
            statement.setString(5,object.get("name").getAsString());
        }catch(SQLException e){
            e.printStackTrace();
        }*/
    }
    public static void main(String[] args){
        ProductDetailCrawler crawler=new ProductDetailCrawler(null,TYPE_TOUR);
        crawler.start();
    }
}
