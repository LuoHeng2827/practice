package com.luoheng.example.xiecheng;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoCrawler extends Crawler{
    private static final String CALENDAR_REQUEST_JSON="{\"ChannelCode\":0,\"PlatformId\":4,\"Version\":\"80400\",\"head\":{\"cid\":\"09031089210204819718\",\"ctok\":\"\",\"cver\":\"1.0\",\"lang\":\"01\",\"sid\":\"8888\",\"syscode\":\"09\",\"auth\":\"\",\"extension\":[]},\"days\":31,\"needAlias\":true,\"needInventory\":true,\"needTradePrice\":true,\"queryTourGroup\":true,\"ProductId\":15927732,\"SaleCityId\":158,\"departureCityId\":158,\"verifyBeginDate\":\"2019-07-02\",\"contentType\":\"json\"}";
    private static final String CALENDAR_URL="https://vacations.ctrip.com/tour/restapi/online/12433/pricecalendarv2?_fxpcqlniredt=09031089210204819718";
    private static final String HOST_URL="https://vacations.ctrip.com";
    private static final String DETAIL_TIMING_REQUEST_JSON="{\"ChannelCode\":0,\"PlatformId\":4,\"Version\":\"80400\",\"head\":{\"cid\":\"09031089210204819718\",\"ctok\":\"\",\"cver\":\"1.0\",\"lang\":\"01\",\"sid\":\"8888\",\"syscode\":\"09\",\"auth\":\"\",\"extension\":[]},\"ProductId\":22427622,\"SaleCityId\":477,\"DepartureCityId\":477,\"QueryNode\":{\"IsBookInfo\":true,\"IsBasicExtendInfo\":true,\"IsDescriptionInfo\":true,\"IsCostInfoList\":true,\"IsSelfPayInfo\":true,\"IsFlightInfo\":true,\"IsTravelIntroductionInfo\":true,\"IsPriceInfo\":true,\"IsVisa\":true,\"IsOrderKnow\":true,\"IsLeaderInfo\":true},\"contentType\":\"json\"}";
    private static final String DETAIL_TIMING_URL="https://vacations.ctrip.com/tour/restapi/online/12447/ProductDetailTimingV5?_fxpcqlniredt=09031089210204819718";
    public static final String FROM_QUEUE="list_xiecheng_product_all_href";
    public static final String TO_QUEUE="list_xiecheng_db";
    private Logger logger=LogManager.getLogger(InfoCrawler.class);
    private Gson gson=new Gson();
    public InfoCrawler(CrawlerFactory factory){
        super(factory);
    }

    public InfoCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public InfoCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }

    private String parseLinkId(String productLink){
        Pattern pattern=Pattern.compile(".*p(.*)s.*");
        Matcher matcher=pattern.matcher(productLink);
        if(!matcher.matches()){
            logger.error("error!!!!");
        }
        return matcher.group(1);
    }

    private String parseCityCode(String productLink){
        Pattern pattern=Pattern.compile(".*s(.*).html.*");
        Matcher matcher=pattern.matcher(productLink);
        if(!matcher.matches()){
            logger.error("error!!!!");
            logger.error(productLink);
        }
        return matcher.group(1);
    }

    private String buildDetailTimingRequestJson(String productId,String departureCityId,int packageNum){
        String[] packageLetter={"A","B","C","D","E","F","G"};
        JsonObject jsonObject=gson.fromJson(DETAIL_TIMING_REQUEST_JSON,JsonObject.class);
        jsonObject.addProperty("ProductId",productId);
        jsonObject.addProperty("departureCityId",departureCityId);
        JsonArray Alias=new JsonArray();
        for(int i=0;i<packageNum;i++){
            Alias.add(packageLetter[i]);
        }
        jsonObject.add("Alias",Alias);
        return gson.toJson(jsonObject);
    }

    private String buildCalendarRequestJson(String productId,String departureCityId){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
        JsonObject jsonObject=gson.fromJson(CALENDAR_REQUEST_JSON,JsonObject.class);
        jsonObject.addProperty("ProductId",productId);
        jsonObject.addProperty("departureCityId",departureCityId);
        jsonObject.addProperty("verifyBeginDate",format.format(new Date()));
        return gson.toJson(jsonObject);
    }


    private boolean crawlCalendar(String productLink,Bean bean) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        String cityCode=parseCityCode(productLink);
        String productId=parseLinkId(productLink);
        StringEntity entity=new StringEntity(buildCalendarRequestJson(productId,cityCode),Charset.forName("UTF-8"));
        entity.setContentType("application/json");
        HttpResponse response=HttpClientUtil.doPost(CALENDAR_URL,params,headers,entity,true,number);
        int responseCode=response.getStatusLine().getStatusCode();
        if(responseCode==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonElement dailyMinPrices=jsonObject.get("dailyMinPrices");
            JsonArray calendarArray=dailyMinPrices.getAsJsonArray();
            int day=Math.min(7,calendarArray.size());
            for(int i=0;i<day;i++){
                Bean.Price bPrice=bean.newPrice();
                bean.priceList.add(bPrice);
                JsonObject item=calendarArray.get(i).getAsJsonObject();
                String date=item.get("date").getAsString();
                float price=item.get("price").getAsFloat();
                bPrice.date=date;
                bPrice.price=price;
            }
        }
        else{
            logger.info("failed,code is "+responseCode);
            return false;
        }
        return true;
    }

    private boolean crawlBasicInfo(String url,Bean bean) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        HttpResponse response=HttpClientUtil.doGet(url,params,headers,true,number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            if(document.getElementsByTag("h1").size()>0)
                bean.productName=document.getElementsByTag("h1").get(0).text();
            else
                return false;
            //获得城市名称
            if(document.getElementsByClass("from_city").size()>0){
                Element from_city=document.getElementsByClass("from_city").get(0);
                String s1=from_city.ownText().replace("出发地:","");
                bean.bPackage.cityName=s1.substring(0,s1.indexOf("("));
            }
            else{
                Element prd_num=document.getElementsByClass("prd_num").get(0);
                bean.bPackage.cityName=prd_num.ownText().replace("出发地:","");
            }
            Elements dls=document.getElementsByTag("dl");
            for(Element dl:dls){
                if(dl.text().contains("供应商"))
                    if(dl.getElementsByTag("span").size()>0)
                        bean.taName=dl.getElementsByTag("span").get(0).ownText();
                    else
                        bean.taName="";
            }
            if(bean.taName==null||bean.taName.length()==0)
                bean.taName="";
        }
        else{
            logger.warn("failed to request "+url+",code is "+code);
            return false;
        }
        return true;
    }

    private int getPackageNumber(String productLink) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(productLink,null,null,true,number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            if(document.getElementsByClass("route_tab_list").size()>0){
                Element route_tab_list=document.getElementsByClass("route_tab_list").get(0);
                return route_tab_list.getElementsByTag("li").size();
            }
            else
                return 0;
        }
        else
            return -1;
    }

    private boolean crawlPath(String productLink,Bean bean) throws Exception{
        String productId=parseLinkId(productLink);
        String cityCode=parseCityCode(productLink);
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        int packageNum=getPackageNumber(productLink);
        StringEntity entity=new StringEntity(buildDetailTimingRequestJson(productId,cityCode,packageNum),
                Charset.forName("UTF-8"));
        entity.setContentType("application/json");
        HttpResponse response=HttpClientUtil.doPost(DETAIL_TIMING_URL,params,headers,entity,true,number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject Data=jsonObject.getAsJsonObject("Data");
            JsonArray TravelIntroductionList=Data.getAsJsonObject("TravelIntroductionInfo")
                    .getAsJsonArray("TravelIntroductionList");
            for(int i=0;i<TravelIntroductionList.size();i++){
                JsonObject pathItem=TravelIntroductionList.get(i).getAsJsonObject();
                if(pathItem.get("Name") instanceof JsonNull)
                    bean.bPackage.name=pathItem.get("Alias").getAsString();
                else
                    bean.bPackage.name=pathItem.get("Alias")+" "+pathItem.get("Name").getAsString();
                JsonArray IntroductionInfoList=pathItem.getAsJsonArray("IntroductionInfoList");
                StringBuilder builder=new StringBuilder();
                for(int j=0;j<IntroductionInfoList.size();j++){
                    pathItem=IntroductionInfoList.get(j).getAsJsonObject();
                    builder.append("D");
                    builder.append(j+1);
                    builder.append(":");
                    builder.append(pathItem.get("Desc").getAsString());
                }
                bean.bPackage.path=builder.toString();
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
            }
        }
        else{
            logger.warn("failed to request "+DETAIL_TIMING_URL+",code is "+code);
            return false;
        }
        return true;
    }

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        String productId=parseLinkId(taskData);
        bean.productId=productId;
        bean.productLink=taskData;
        try{
            if(!crawlBasicInfo(taskData,bean)||!crawlCalendar(taskData,bean)||!crawlPath(taskData,bean)){
                JedisUtil.lpush(FROM_QUEUE,taskData);
                return;
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
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("https://vacations.ctrip.com/tour/detail/p10149493s477.html");
    }
}
