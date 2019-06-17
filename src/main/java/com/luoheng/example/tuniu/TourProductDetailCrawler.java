package com.luoheng.example.tuniu;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TourProductDetailCrawler extends Crawler {
    private static final String FROM_TOUR_QUEUE="list_tuniu_tour_product_id";
    private static final String HTML_URL="https://m.tuniu.com/tour/%s";
    private static final String CITY_URL="https://m.tuniu.com/wap-detail/api/group/city";
    private static final String DETAIL_URL="https://m.tuniu.com/wap-detail/api/self/detail/getProductInfo";
    private static final String PRICE_URL="https://m.tuniu.com/wap-detail/api/group/price";
    private static final String CALENDAR_URL="https://m.tuniu.com/wap-detail/api/group/calendar";
    private static final String JOURNEY_URL="https://m.tuniu.com/wap-detail/api/group/journey";
    private static final String TEMPLATE_TOUR_PRODUCT_LINK="http://www.tuniu.com/tour/%s";
    private Gson gson;
    private Logger logger= LogManager.getLogger(TourProductDetailCrawler.class);
    public TourProductDetailCrawler(CrawlerFactory factory) {
        super(factory);
    }

    public TourProductDetailCrawler(CrawlerFactory factory, String name) {
        super(factory,name);
    }

    public TourProductDetailCrawler(CrawlerFactory factory, String name, long crawlInterval) {
        super(factory,name,crawlInterval);
    }

    @Override
    public void init() {
        super.init();
        gson=new Gson();
    }

    @Override
    public String getTaskData() {
        Jedis jedis=JedisUtil.getResource();
        String productId;
        productId=jedis.lpop(FROM_TOUR_QUEUE);
        jedis.close();
        return productId;
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

    private boolean getProductBasicInfo(String taskData,JsonObject result) throws IOException {
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("d", String.format("{\"productId\":\"%s\",\"journeyId\":0,\"bookCityCode\":2500}", taskData));
        headers.put("User-Agent", "Chrome/74.0.3729.169 Mobile");
        headers.put("Referer", "https://m.tuniu.com/h5/package/"+taskData);
        Response response=HttpUtil.doGet(DETAIL_URL, params, headers);
        if(response.code()==200) {
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr, JsonObject.class);
            if(jsonObject.get("data") instanceof JsonNull){
                return false;
            }
            JsonObject data=jsonObject.getAsJsonObject("data");
            result.add("name", data.get("name"));
        }
        else{
            logger.info("failed to request "+response.request().url());
            return false;
        }
        return true;
    }

    private void getProductVendor(Map<String,Integer> cityInfo, String taskData, JsonObject result)
            throws IOException{
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
                if(jsonObject.get("data") instanceof JsonNull){
                    return;
                }
                JsonObject data=jsonObject.getAsJsonObject("data");
                JsonObject vendorInfo=data.getAsJsonObject("vendorInfo");
                JsonElement companyName=vendorInfo.get("companyName");
                if(companyName.getAsString()==null)
                    cityResult.addProperty("companyName", "跟团游");
                else
                    cityResult.addProperty("companyName", companyName.getAsString());
                cityResult.addProperty("fullName", vendorInfo.get("fullName").getAsString());
                if(!getProductCalendar(taskData,cityCode,cityResult))
                    continue;
                cityArrayResult.add(cityResult);
            } else {
                logger.info("failed to request "+response.request().url());
                return;
            }
        }
        result.add("cityList",cityArrayResult);
    }
    private boolean getProductCalendar(String taskData,int cityCode,JsonObject result) throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("productId",taskData);
        params.put("bookCityCode",cityCode+"");
        Response response=HttpUtil.doGet(CALENDAR_URL,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            if(data.get("calendarInfos") instanceof JsonNull){
                /*logger.info(response.request().url());
                logger.info(responseStr);*/
                return false;
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
            return false;
        }
        return true;
    }

    /*private void getProductHtml(String taskData,JsonObject result) throws IOException{
        Response response=HttpUtil.doGet(String.format(HTML_URL,taskData    ),null,null);
        if(response.code()==200){
            String responseStr=response.body().string();
            Document document=Jsoup.parse(responseStr);
            Elements scripts=document.getElementsByTag("script");
            for(Element script:scripts){
                if(script.text().contains("window.routeData")){
                    Pattern pattern=Pattern.compile("window.routeData = \\{(.*)\\}");
                    Matcher matcher=pattern.matcher(script.text());
                    if(matcher.matches()){

                    }
                }
            }
        }
        else{
            logger.info("failed to request "+response.request().url());
        }
    }*/

    private void getProductJourney(String taskData,JsonObject result) throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        params.put("productId",taskData);
        Response response=HttpUtil.doGet(JOURNEY_URL,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            if(data.get("journeyDetailList") instanceof JsonNull){
                logger.info(response.request().url());
                logger.info(responseStr);
                return;
            }
            JsonArray journeyDetailList=data.getAsJsonArray("journeyDetailList");
            //journey detail result
            JsonArray journeyListResult=new JsonArray();
            result.addProperty("destination",journeyDetailList.get(0)
                    .getAsJsonObject().get("destination").getAsString());
            for(int i=0;i<journeyDetailList.size();i++){
                //journey detail result
                JsonArray journeyDetailResult=new JsonArray();
                JsonObject journeyBaseInfo=journeyDetailList.get(i).getAsJsonObject()
                        .getAsJsonObject("journeyBaseInfo");
                result.addProperty("teamCityName",journeyBaseInfo.get("teamCityName").getAsString());
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
        result.addProperty("productLink", String.format(TEMPLATE_TOUR_PRODUCT_LINK,taskData));
        result.addProperty("type","tour");
        try{
            Map<String,Integer> cityInfo=getCityInfo(taskData);
            if(!getProductBasicInfo(taskData,result))
                return;
            getProductJourney(taskData,result);
            getProductVendor(cityInfo,taskData,result);
            saveData(result);
        }catch(IOException e){
            e.printStackTrace();
        }
        logger.info("end");
    }

    private String buildJourneyString(JsonArray array){
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<array.size();i++){
            builder.append("D");
            builder.append(i+1);
            builder.append(":");
            JsonObject item=array.get(i).getAsJsonObject();
            builder.append(item.get("journeyDescription").getAsString());
        }
        return builder.toString();
    }

    private void saveData(JsonObject object){
        try{
            Connection connection=DBPoolUtil.getConnection();

            PreparedStatement statement=connection.prepareStatement("INSERT INTO TRAVEL_PRODUCT_INFO("+
                    "`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`,`TA_NAME`,`PACKAGE_NAME`,"+
                    "`TRAVEL_PLAN`,`TOUR_PARTY`,`PARYT_PALCE`,`TOUR_DESTN`,`TOUR_DAYS`,`TOUR_PATH`)" +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
            statement.setString(1, ThreadUtil.getUUID());
            statement.setInt(2,6);
            statement.setString(3,object.get("type").getAsString());
            statement.setString(4,object.get("productId").getAsString());
            statement.setString(5,object.get("name").getAsString());
            JsonObject firstCityItem=object.getAsJsonArray("cityList")
                    .get(0).getAsJsonObject();
            statement.setString(6,firstCityItem.get("fullName").getAsString());
            statement.setString(7,firstCityItem.get("companyName").getAsString());
            statement.setString(8,buildJourneyString
                    (object.getAsJsonArray("journeyList")));
            statement.setString(9,object.get("teamCityName").getAsString());
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        TourProductDetailCrawler crawler=new TourProductDetailCrawler(null);
        crawler.start();
    }
}
