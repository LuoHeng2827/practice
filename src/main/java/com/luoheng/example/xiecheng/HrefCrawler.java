package com.luoheng.example.xiecheng;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过产品链接来获取该产品不同城市下的链接
 */
public class HrefCrawler extends Crawler{
    private static final String TEMPLATE_URL="https://vacations.ctrip.com/tour/detail/p%ss%s.html";
    private static final String DETAIL_TIMING_URL="https://vacations.ctrip.com/tour/restapi/online/12447/ProductDetailTimingV5?_fxpcqlniredt=09031089210204819718";
    private static final String DETAIL_TIMING_REQUEST_JSON="{\"ChannelCode\":0,\"PlatformId\":4,\"Version\":\"80400\",\"head\":{\"cid\":\"09031089210204819718\",\"ctok\":\"\",\"cver\":\"1.0\",\"lang\":\"01\",\"sid\":\"8888\",\"syscode\":\"09\",\"auth\":\"\",\"extension\":[]},\"ProductId\":22427622,\"SaleCityId\":477,\"DepartureCityId\":477,\"QueryNode\":{\"IsBookInfo\":true,\"IsBasicExtendInfo\":true,\"IsDescriptionInfo\":true,\"IsCostInfoList\":true,\"IsSelfPayInfo\":true,\"IsFlightInfo\":true,\"IsTravelIntroductionInfo\":true,\"IsPriceInfo\":true,\"IsVisa\":true,\"IsOrderKnow\":true,\"IsLeaderInfo\":true},\"contentType\":\"json\"}";
    public static final String FROM_QUEUE="list_xiecheng_product_href";
    public static final String TO_QUEUE="list_xiecheng_product_all_href";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(HrefCrawler.class);
    public HrefCrawler(CrawlerFactory factory){
        super(factory);
    }

    public HrefCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public HrefCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    /**
     * 构建请求DETAIL_TIMING_URL的json参数
     * @param productId
     * @return
     */
    private String buildDetailTimingRequestJson(String productId){
        JsonObject jsonObject=gson.fromJson(DETAIL_TIMING_REQUEST_JSON,JsonObject.class);
        jsonObject.addProperty("ProductId",productId);
        return gson.toJson(jsonObject);
    }

    /**
     *
     * @param productId
     * @return
     * @throws Exception
     */
    private boolean crawlAllLink(String productId) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        StringEntity entity=new StringEntity(buildDetailTimingRequestJson(productId),Charset.forName("UTF-8"));
        entity.setContentType("application/json");
        HttpResponse response=HttpClientUtil.doPost(DETAIL_TIMING_URL,params,headers,entity,
                Boolean.parseBoolean(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject Data=jsonObject.getAsJsonObject("Data");
            if(Data.getAsJsonObject("PriceInfo").get("DepartureCityPriceList") instanceof JsonNull){
                JedisUtil.lpush(TO_QUEUE,getTaskData());
                return true;
            }
            JsonArray DepartureCityPriceList=Data.getAsJsonObject("PriceInfo")
                    .getAsJsonArray("DepartureCityPriceList");
            for(int i=0;i<DepartureCityPriceList.size();i++){
                JsonObject item=DepartureCityPriceList.get(i).getAsJsonObject();
                String cityId=item.get("DepartureCityId").getAsString();
                JsonArray productList=item.get("ProductIdList").getAsJsonArray();
                for(int j=0;j<productList.size();j++){
                    String url=String.format(TEMPLATE_URL,productList.get(j).getAsString(),cityId);
                    JedisUtil.lpush(TO_QUEUE,url);
                }
            }
        }
        else{
            logger.warn("failed to request "+DETAIL_TIMING_URL+",code is "+code);
            return false;
        }
        return true;
    }



    private String parseLinkId(String productLink){
        Pattern pattern=Pattern.compile(".*p(.*)s.*");
        Matcher matcher=pattern.matcher(productLink);
        if(!matcher.matches()){
            logger.error("error!!!!");
        }
        return matcher.group(1);
    }

    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }

    @Override
    public void crawl(String taskData){
        String productId=parseLinkId(taskData);
        try{
            if(!crawlAllLink(productId)){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
        }catch(Exception e){
            JedisUtil.lpush(FROM_QUEUE,taskData);
            if(e instanceof IOException){
                e.printStackTrace();
            }
            else{
                Core.saveErrorMsg(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        HrefCrawler crawler=new HrefCrawler(null);
        crawler.crawl("https://vacations.ctrip.com/tour/detail/p23744851s158.html");
    }
}
