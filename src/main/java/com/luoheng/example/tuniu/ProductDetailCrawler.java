package com.luoheng.example.tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import okhttp3.Response;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProductDetailCrawler extends Crawler {
    private static final String FROM_TOUR_QUEUE="list_tuniu_tour_product_id";
    private static final String FROM_PKG_QUEUE="list_tuniu_pkg_product_id";
    private static final String DETAIL_URL ="https://m.tuniu.com/wap-detail/api/self/detail/getProductInfo";
    private static final String PRICE_URL="https://m.tuniu.com/wap-detail/api/group/price";
    private static final String CALENDAR_URL="https://m.tuniu.com/wap-detail/api/group/calendar";
    private static final String JOURNEY_URL="https://m.tuniu.com/wap-detail/api/group/journey";
    private static final String TEMPLATE_TOUR_PRODUCT_LINK="http://www.tuniu.com/tour/%s";
    private static final String TEMPLATE_PKG_PRODUCT_LINK="http://www.tuniu.com/package/%s";
    public static final int TYPE_TOUR=0;
    public static final int TYPE_PKG=1;
    private int type;
    private Gson gson;
    public ProductDetailCrawler(CrawlerController controller,int type) {
        super(controller);
        this.type=type;
    }

    public ProductDetailCrawler(CrawlerController controller, String name,int type) {
        super(controller, name);
        this.type=type;
    }

    public ProductDetailCrawler(CrawlerController controller, String name, long crawlInterval,int type) {
        super(controller, name, crawlInterval);
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
        while(true){
            String productId;
            if(type==TYPE_TOUR)
                productId=jedis.lpop(FROM_TOUR_QUEUE);
            else
                productId=jedis.lpop(FROM_PKG_QUEUE);
            if(productId!=null)
                return productId;
            if(isOver()){
                interrupt();
            }
            ThreadUtil.waitMillis(1000);
        }
    }

    /**
     * 获得出发地可选城市及编号
     * @param secondCityList
     * @return
     */
    private Map<String,Integer> getCityMap(JsonArray secondCityList){
        Map<String,Integer> cityMap=new HashMap<>();
        for(int i=0;i<secondCityList.size();i++){
            JsonObject jsonObject=secondCityList.get(i).getAsJsonObject();
            JsonArray thirdCityList=jsonObject.getAsJsonArray("thirdCityList");
            for(int j=0;j<thirdCityList.size();j++){
                JsonObject object=thirdCityList.get(i).getAsJsonObject();
                cityMap.put(object.get("bookCityName").getAsString(),object.get("bookCityCode").getAsInt());
            }
        }
        return cityMap;
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
        Map<String,Integer> cityMap=new HashMap<>();
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("d", String.format("{\"productId\":\"%s\",\"journeyId\":0,\"bookCityCode\":2500}",taskData));
        headers.put("User-Agent","Chrome/74.0.3729.169 Mobile");
        headers.put("Referer","https://m.tuniu.com/h5/package/"+taskData);
        try{
            Response response=HttpUtil.doGet(DETAIL_URL,params,headers);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                String name=data.get("name").getAsString();
                result.addProperty("name",name);
                cityMap=getCityMap(data.getAsJsonObject("departCities")
                        .getAsJsonObject("allCities").getAsJsonArray("secondCityList"));
            }
            else
                return;
            /**
             * 对每个出发地遍历
             */
            for(Map.Entry<String,Integer> entry:cityMap.entrySet()){
                String cityName=entry.getKey();
                int cityCode=entry.getValue();
                params=new HashMap<>();
                params.put("productId",taskData);
                params.put("bookCity",cityCode+"");
                response=HttpUtil.doGet(PRICE_URL,params,headers);
                if(response.code()==200){
                    String responseStr=response.body().string();
                    JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                    JsonObject data=jsonObject.getAsJsonObject("data");
                    JsonObject vendorInfo=data.getAsJsonObject("vendorInfo");
                    JsonObject companyName=vendorInfo.getAsJsonObject("companyName");
                    if(companyName.getAsString()==null)
                        result.addProperty("companyName","null");
                    else
                        result.addProperty("companyName",companyName.getAsString());
                    result.addProperty("fullName",vendorInfo.get("fullName").getAsString());
                }
                else{
                    //todo
                }
                response=HttpUtil.doGet(CALENDAR_URL,params,headers);
                if(response.code()==200){
                    String responseStr=response.body().string();
                    JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                    JsonObject data=jsonObject.getAsJsonObject("data");
                    JsonArray calendarInfos=data.getAsJsonArray("calendarInfos");
                    JsonArray calendarResult=new JsonArray();
                    for(int i=0;i<calendarInfos.size();i++){
                        JsonArray calendarDetails=calendarInfos.get(i)
                                .getAsJsonObject().getAsJsonArray("calendarDetails");
                        for(int j=0;j<calendarDetails.size();i++){
                            JsonObject object=calendarDetails.get(i).getAsJsonObject();
                            JsonObject objectResult=new JsonObject();
                            objectResult.addProperty("planDate",object.get("planDate").getAsString());
                            objectResult.addProperty("adultPrice",object.get("adultPrice").getAsString());
                            calendarResult.add(objectResult);
                        }
                    }
                    result.add("calendar",calendarResult);
                }
                else{
                    //todo
                }
                response=HttpUtil.doGet(JOURNEY_URL,params,headers);
                if(response.code()==200){
                    String responseStr=response.body().string();
                    JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                    JsonObject data=jsonObject.getAsJsonObject("data");
                }
                else{
                    //todo
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
