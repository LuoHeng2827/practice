package com.luoheng.example.tuniu;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.http.OkHttpUtil;
import com.luoheng.example.util.redis.JedisUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TourProductDetailCrawler extends Crawler {
    private static final String FROM_QUEUE="list_tuniu_tour_product_id";
    private static final String TO_QUEUE="list_tuniu_tour_db";
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
        productId=jedis.lpop(FROM_QUEUE);
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
        Response response=OkHttpUtil.doGet(CITY_URL,params,headers);
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
        Response response=OkHttpUtil.doGet(DETAIL_URL, params, headers);
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
            Response response=OkHttpUtil.doGet(PRICE_URL, params, headers);
            if(response.code()==200) {
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr, JsonObject.class);
                if(jsonObject.get("data") instanceof JsonNull){
                    return;
                }
                JsonObject data=jsonObject.getAsJsonObject("data");
                JsonObject vendorInfo=data.getAsJsonObject("vendorInfo");
                JsonElement companyName=vendorInfo.get("companyName");
                if(companyName instanceof JsonNull)
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
        Response response=OkHttpUtil.doGet(CALENDAR_URL,params,headers);
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
        Response response=OkHttpUtil.doGet(String.format(HTML_URL,taskData    ),null,null);
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
        Response response=OkHttpUtil.doGet(JOURNEY_URL,params,headers);
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
            //journey plan list result
            JsonArray journeyListResult=new JsonArray();
            for(int i=0;i<journeyDetailList.size();i++){
                //journey item result
                JsonObject journeyResult=new JsonObject();
                //journey detail result
                JsonArray journeyDetailResult=new JsonArray();
                JsonObject journeyBaseInfo=journeyDetailList.get(i).getAsJsonObject()
                        .getAsJsonObject("journeyBaseInfo");
                journeyResult.addProperty("destination",journeyDetailList.get(i)
                        .getAsJsonObject().get("destination").getAsString());
                journeyResult.addProperty("dayDuration",journeyBaseInfo.get("dayDuration").getAsInt());
                journeyResult.addProperty("nightDuration",journeyBaseInfo.get("nightDuration").getAsInt());
                journeyResult.addProperty("teamCityName",journeyBaseInfo.get("teamCityName").getAsString());
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
                journeyResult.add("journeyDetail",journeyDetailResult);
                journeyListResult.add(journeyResult);
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
            Jedis jedis=JedisUtil.getResource();
            jedis.lpush(TO_QUEUE,result.toString());
            jedis.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        logger.info("end");
    }
    public static void main(String[] args){
        TourProductDetailCrawler crawler=new TourProductDetailCrawler(null);
        crawler.start();
    }
}
