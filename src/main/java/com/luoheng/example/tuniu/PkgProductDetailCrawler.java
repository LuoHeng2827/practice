package com.luoheng.example.tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
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

public class PkgProductDetailCrawler extends Crawler{
    private static final String FROM_PKG_QUEUE="list_tuniu_pkg_product_id";
    private static final String DETAIL_URL="https://m.tuniu.com/wap-detail/api/self/detail/getProductInfo";
    private static final String PRICE_URL="https://m.tuniu.com/wap-detail/api/self/detail/getPriceInfo";
    private static final String CALENDAR_URL="https://m.tuniu.com/wap-detail/api/self/detail/getCalendarInfo";
    private static final String CITY_URL="https://m.tuniu.com/wap-detail/api/self/detail/getCityInfo";
    private static final String TEMPLATE_PKG_PRODUCT_LINK="http://www.tuniu.com/package/%s";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(PkgProductDetailCrawler.class);
    public PkgProductDetailCrawler(CrawlerFactory factory){
        super(factory);
    }

    public PkgProductDetailCrawler(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public PkgProductDetailCrawler(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    @Override
    public String getTaskData(){
        Jedis jedis=JedisUtil.getResource();
        String productId;
        productId=jedis.lpop(FROM_PKG_QUEUE);
        jedis.close();
        return productId;
        //return "210713611";
    }

    private Map<String,Integer> getCityInfo(String taskData) throws IOException{
        Map<String,Integer> cityMap=new HashMap<>();
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("d", String.format("{\"productId\":\"%s\"}", taskData));
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

    private boolean getProductBasicInfo(String taskData,JsonObject result) throws IOException{
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
            JsonArray journeyList=data.getAsJsonArray("journeyList");
            result.add("journeyList",journeyList);
        }
        else{
            logger.info("failed to request "+response.request().url());
            return false;
        }
        return true;
    }

    private void getProductCalendar(String taskData,Map<String,Integer> cityMap,JsonObject result)
            throws IOException{
        Map<String, String> params=new HashMap<>();
        Map<String, String> headers=new HashMap<>();
        headers.put("User-Agent", "Chrome/74.0.3729.169 Mobile");
        headers.put("Referer", "https://m.tuniu.com/h5/package/"+taskData);
        JsonArray cityListResult=new JsonArray();
        for(Map.Entry<String,Integer> entry:cityMap.entrySet()){
            String cityName=entry.getKey();
            int cityCode=entry.getValue();
            JsonArray calendarListResult=new JsonArray();
            JsonObject cityResult=new JsonObject();
            params.put("d", String.format(
                    "{\"productId\":\"%s\",\"productType\":108,\"bookCityCode\":%s}", taskData,cityCode));
            Response response=HttpUtil.doGet(CALENDAR_URL, params, headers);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                JsonArray calendarInfo=data.getAsJsonArray("calendarInfo");
                for(int i=0;i<calendarInfo.size();i++){
                    JsonObject calendarItem=calendarInfo.get(i).getAsJsonObject();
                    JsonObject calendarResult=new JsonObject();
                    calendarResult.addProperty("adultPrice",calendarItem.get("adultPrice").getAsString());
                    calendarResult.addProperty("planDate",calendarItem.get("planDate").getAsString());
                    calendarListResult.add(calendarResult);
                }
                cityResult.addProperty("cityName",cityName);
                cityResult.add("calendar",calendarListResult);
                cityListResult.add(cityResult);

            }
            else{
                logger.info("failed to request "+response.request().url());
            }
        }
        result.add("cityList",cityListResult);
    }

    private String buildPackageStr(JsonArray array){
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<array.size();i++){
            JsonObject object=array.get(i).getAsJsonObject();
            builder.append(object.get("journeyName").getAsString());
            builder.append("|");
        }
        builder.deleteCharAt(builder.length());
        return builder.toString();
    }

    private void saveData(JsonObject object){
        logger.info(object.toString());

        try{
            Connection connection=DBPoolUtil.getConnection();
            PreparedStatement statement=connection.prepareStatement("INSERT INTO TRAVEL_PRODUCT_INFO("+
                    "`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`,`PACKAGE_NAME`,`PROD_LINK`)" +
                    "VALUES(?,?,?,?,?,?,?) ");
            statement.setString(1,ThreadUtil.getUUID());
            statement.setInt(2,6);
            statement.setString(3,object.get("type").getAsString());
            statement.setString(4,object.get("productId").getAsString());
            statement.setString(5,object.get("name").getAsString());
            statement.setString(6,buildPackageStr(object.getAsJsonArray("journeyList")));
            statement.setString(7,object.get("productLink").getAsString());
            statement.execute();
            statement.close();
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void crawl(String taskData){
        JsonObject result=new JsonObject();
        result.addProperty("productId",taskData);
        result.addProperty("productLink", String.format(TEMPLATE_PKG_PRODUCT_LINK,taskData));
        result.addProperty("type","pkg");
        try{
            if(!getProductBasicInfo(taskData,result)){
                return;
            }
            Map<String,Integer> cityMap=getCityInfo(taskData);
            getProductCalendar(taskData,cityMap,result);
            saveData(result);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        PkgProductDetailCrawler crawler=new PkgProductDetailCrawler(null);
        crawler.start();
    }
}
