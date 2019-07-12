package com.luoheng.example.lvmama;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.PropertiesUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InfoCrawler extends Crawler{
    private static final int DEFAULT_CALENDAR_DAY=7;
    public static final String FROM_QUEUE="lvmama_product_href";
    public static final String TO_QUEUE="lvmama_product_db";

    private static final String CALENDAR_URL_OLD="http://dujia.lvmama.com/vst_front/route/data.json?productId=%s&startDistrictId=%s&businessType=DestBu";
    private static final String PATH_URL_NEW="http://vacations.lvmama.com/tims-web/w/product/queryProductExplainInfo?productId=%S&lvsign=E8CCE375075C32C8BD08CE80B52CA8BE1D6A3ACE&_=1562636679708";
    private static final String CALENDAR_URL_NEW="http://vacations.lvmama.com/w/product/querySchedule?productId=%s";
    private Gson gson=new Gson();
    private int count=1;
    private Logger logger=LogManager.getLogger(InfoCrawler.class);
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


    //爬取以dujia为二级域名的页面的基本信息
    private boolean crawlInfoOld(String productUrl,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            bean.productName=document.getElementsByClass("detail_product_tit")
                    .get(0).getElementsByTag("b").get(0).ownText();
            bean.productId=document.getElementsByClass("detail_product_num")
                    .get(0).getElementsByTag("i").get(0).ownText().replace("产品编号：","");
            bean.productLink=productUrl;
            bean.taName=document.getElementsByClass("detail_product_num")
                    .get(0).ownText().replace("本产品委托社为","")
                    .replace("，具体的旅游服务和操作由委托社提供。","").trim();
            bean.bPackage.cityName=document.getElementsByClass("product_top_booking")
                    .get(0).getElementsByTag("dd").get(0).ownText().trim();
            bean.bPackage.name="A行程";
            Element pathElement=document.getElementsByClass("instance_list").get(0)
                    .getElementsByTag("dl").last();
            Elements ps=pathElement.getElementsByTag("p");
            StringBuilder builder=new StringBuilder();
            for(int i=0;i<ps.size();i++){
                Element p=ps.get(i);
                builder.append("D");
                builder.append(i+1);
                builder.append(":");
                builder.append(p.ownText().trim().replaceAll("【第\\d天】",""));
            }
            bean.bPackage.path=builder.toString();
            String cityCode=document.getElementById("product-recommend").attr("districtid");
            return crawlCalendarOld(bean.productId,cityCode,bean);
        }
        else{
            logger.info("failed to request,code is "+code);
            return false;
        }
    }

    //爬取以dujia为二级域名的页面的基本信息
    private boolean crawlCalendarOld(String productId,String cityCode,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(CALENDAR_URL_OLD,productId,cityCode),null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonArray calendarInfo=jsonObject.getAsJsonArray("calendarInfo");
            int day=Math.min(7,calendarInfo.size());
            for(int i=0;i<day;i++){
                JsonObject calendarItem=calendarInfo.get(i).getAsJsonObject();
                String date=calendarItem.get("date").getAsString();
                float price=calendarItem.get("price").getAsFloat();
                bean.priceList.add(bean.newPrice(date,price));
            }
        }
        else{
            logger.info("failed to request,code is "+code);
            return false;
        }
        return true;
    }

    //爬取以vacation为二级域名的页面的基本信息
    private boolean crawlInfoNew(String productUrl,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doPost(productUrl,null,null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            bean.productName=document.getElementsByTag("h2").get(0)
                    .getElementsByTag("em").get(0).ownText()
                    .replaceAll("【.*】","").replaceAll("\\(.*\\)","");
            bean.productLink=productUrl;
            bean.productId=document.getElementsByClass("id-num").get(0).getElementsByTag("em").get(0).ownText();
            bean.taName=document.getElementsByClass("id-supp").get(0).getElementsByTag("em").get(0).ownText();
            bean.bPackage.cityName=document.getElementsByClass("d-info").get(0)
                    .getElementsByClass("info-city").get(0).getElementsByTag("ii").get(0).ownText();
        }
        else{
            logger.info("failed to request,code is "+code);
            return false;
        }
        return true;
    }

    //爬取以vacation为二级域名的页面的基本信息
    private boolean crawlPathNew(Bean bean,List<List<Bean.Price>> bPriceLists) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(PATH_URL_NEW,bean.productId),null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonArray productTraceInfos=jsonObject.getAsJsonArray("productTraceInfos");
            for(int i=0;i<productTraceInfos.size();i++){
                JsonObject productTraceInfo=productTraceInfos.get(i).getAsJsonObject();
                bean.bPackage.name=productTraceInfo.get("productPackageAlias").getAsString();
                JsonArray strokeDescriptionList=productTraceInfo.getAsJsonArray("strokeDescriptionList");
                StringBuilder builder=new StringBuilder();
                for(int j=0;j<strokeDescriptionList.size();j++){
                    JsonArray productDailyDescItemNewDTOList=strokeDescriptionList.get(j)
                            .getAsJsonObject().getAsJsonArray("productDailyDescItemNewDTOList");
                    builder.append("D");
                    builder.append(j+1);
                    builder.append(":");
                    for(int k=0;k<productDailyDescItemNewDTOList.size();k++){
                        builder.append(productDailyDescItemNewDTOList.get(k).getAsJsonObject().get("name").getAsString());
                        builder.append("-");
                    }
                    builder.deleteCharAt(builder.length()-1);
                }
                bean.bPackage.path=builder.toString();
                bean.priceList.clear();
                bean.priceList.addAll(bPriceLists.get(i));
                //logger.info(gson.toJson(bean));
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
            }
        }
        else{
            logger.info("falied to request,code is "+code);
            return false;
        }
        return true;
    }

    //爬取以vacation为二级域名的页面的价格日历
    private boolean crawlCalendarNew(Bean bean,List<List<Bean.Price>> bPriceLists) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(CALENDAR_URL_NEW,bean.productId),
                null,null,Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject datePrices=jsonObject.getAsJsonObject("data").getAsJsonObject("datePrices");
            int day=Math.min(DEFAULT_CALENDAR_DAY,datePrices.size());
            int count=0;
            boolean isFirst=true;
            for(Map.Entry<String,JsonElement> datePrice:datePrices.entrySet()){
                if(count>=day)
                    break;
                count++;
                JsonArray packageDatePriceDTOS=datePrice.getValue().getAsJsonObject().getAsJsonArray("packageDatePriceDTOS");
                if(isFirst){
                    int packageSize=packageDatePriceDTOS.size();
                    for(int i=0;i<packageSize;i++){
                        List<Bean.Price> priceList=new ArrayList<>();
                        bPriceLists.add(priceList);
                    }
                    isFirst=false;
                }
                String date=datePrice.getKey();
                for(int j=0;j<packageDatePriceDTOS.size();j++){
                    JsonObject dayPriceDTO=packageDatePriceDTOS.get(j).getAsJsonObject().getAsJsonObject("dayPriceDTO");
                    float price=Float.parseFloat(dayPriceDTO.get("adultPrice").getAsString());
                    Bean.Price bPrice=bean.newPrice(date,price);
                    bPriceLists.get(j).add(bPrice);
                }
            }
        }
        else{
            logger.info("failed to request,code is "+code);
            return false;
        }
        return true;
    }



    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        try{
            if(taskData.contains("vacations.lvmama.com")){
                List<List<Bean.Price>> bPriceLists=new ArrayList<>();
                if(!crawlInfoNew(taskData,bean)||!crawlCalendarNew(bean,bPriceLists)||!crawlPathNew(bean,bPriceLists)){
                    JedisUtil.lpush(FROM_QUEUE,taskData);
                }
            }
            else if(taskData.contains("dujia.lvmama.com")){
                if(!crawlInfoOld(taskData,bean)){
                    JedisUtil.lpush(FROM_QUEUE,taskData);
                }
                else{
                    JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
                }
            }
            else
                logger.info("ignore task "+taskData);
        }catch(Exception e){
            e.printStackTrace();
            if(e instanceof IOException){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
            else{
                Core.saveErrorMsg(taskData+"\n"+ExceptionUtil.getTotal(e));
            }
        }
    }
    public static void main(String[] args) throws Exception{
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("http://vacations.lvmama.com/w/tour/100094713");
    }
}
