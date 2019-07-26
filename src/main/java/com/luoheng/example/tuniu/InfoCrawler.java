package com.luoheng.example.tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InfoCrawler extends Crawler{
    public static final String FROM_QUEUE="tuniu_product_all_link";
    public static final String TO_QUEUE="tuniu_product_db";
    //获得旅行商的url
    private static final String PACKAGE_NAME_URL="http://www.tuniu.com/web-detail/api/group/aggregation";
    //获得价格日历
    private static final String CALENDAR_URL="http://www.tuniu.com/web-detail/api/group/calendar";
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

    private boolean crawlInfo(String productUrl,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            bean.productName=document.getElementsByTag("title").first().ownText();
            Element sub=document.getElementsByClass("resource-title-sub").first();
            bean.productId=sub.getElementsByClass("resource-number").first().ownText()
                    .replace("编号","").replace("：","");
            bean.taName=sub.getElementsByClass("reource-vendor").first().ownText();
            Elements details=document.getElementsByClass("J_DetailFeature section-box detail-feature")
                    .first().getElementsByClass("detail-feature-brief-item");
            bean.duration=details.get(0).getElementsByTag("strong").first().ownText();
            bean.partyPlace=details.get(1).getElementsByTag("strong").first().ownText();
            bean.bPackage.cityName=document
                    .getElementsByClass("resource-city-more-selected")
                    .first().ownText();
            Elements pathDetail=document
                    .getElementsByClass("J_DetailRouteBriefInner detail-route-brief-inner").first()
                    .getElementsByTag("p");
            StringBuilder builder=new StringBuilder();
            for(int i=0;i<pathDetail.size();i++){
                builder.append("D");
                builder.append(i+1);
                builder.append(":");
                builder.append(pathDetail.get(i).ownText().replaceAll("、","-").trim());
            }
            bean.bPackage.path=builder.toString();
        }
        else{
            logger.info("failed to request "+productUrl+",code is "+code);
            return false;
        }
        String cityCode=productUrl.substring(productUrl.lastIndexOf("/")+1,productUrl.length());
        return crawlPackageName(bean,cityCode)&&crawlCalendar(bean,cityCode);
    }

    private boolean crawlPackageName(Bean bean,String cityCode) throws Exception{
        Map<String,String> params=new HashMap<>();
        params.put("supplier","1");
        params.put("productId",bean.productId);
        params.put("destGroupId",cityCode);
        params.put("destGroupName","云南");
        HttpResponse response=HttpClientUtil.doGet(PACKAGE_NAME_URL,params,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            JsonArray rows=data.getAsJsonObject("supplier").getAsJsonArray("rows");
            bean.bPackage.name=rows.get(0).getAsJsonObject().get("aggregationCompanyName").getAsString();
        }
        else{
            logger.info("failed to request "+bean.productLink+",code is "+code);
            return false;
        }
        return true;
    }

    private boolean crawlCalendar(Bean bean,String cityCode) throws Exception{
        Map<String,String> params=new HashMap<>();
        params.put("productId",bean.productId);
        params.put("bookCityCode",cityCode);
        params.put("departCityCode",cityCode);
        params.put("backCityCode",cityCode);
        HttpResponse response=HttpClientUtil.doGet(CALENDAR_URL,params,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject data=jsonObject.getAsJsonObject("data");
            JsonArray calendars=data.getAsJsonArray("calendars");
            int day=Math.min(7,calendars.size());
            for(int i=0;i<day;i++){
                JsonObject calendar=calendars.get(i).getAsJsonObject();
                String date=calendar.get("departDate").getAsString();
                float price=calendar.get("startPrice").getAsFloat();
                bean.priceList.add(bean.newPrice(date,price));
            }
        }
        else{
            logger.info("failed to request "+CALENDAR_URL+",code is "+code);
            return false;
        }
        return true;
    }

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        bean.productLink=taskData;
        try{
            if(crawlInfo(taskData,bean)){
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
                //logger.info(gson.toJson(bean));
            }
            else{
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
        }catch(Exception e){
            e.printStackTrace();
            if(e instanceof IOException){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
            else{
                Core.saveErrorMsg(taskData+"\n"+ExceptionUtil.getTotal(e));
            }
        }
        ThreadUtil.waitMillis(1000);
    }
    public static void main(String[] args){
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("http://www.tuniu.com/tour/210443228/1202/1202/1202");
    }
}
