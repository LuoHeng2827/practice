package com.luoheng.example.lvmama;

import com.google.gson.*;
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
import java.util.*;

public class InfoCrawler extends Crawler{
    private static final int DEFAULT_CALENDAR_DAY=7;
    public static final String FROM_QUEUE="lvmama_product_href";
    public static final String TO_QUEUE="lvmama_product_db";

    //以dujia为二级域名价格日历的url
    private static final String CALENDAR_URL_OLD="http://dujia.lvmama.com/vst_front/route/data.json?productId=%s&startDistrictId=%s&businessType=DestBu";
    //获得路线信息的url
    private static final String PATH_URL_NEW="http://vacations.lvmama.com/tims-web/w/product/queryProductExplainInfo?productId=%S&lvsign=E8CCE375075C32C8BD08CE80B52CA8BE1D6A3ACE&_=1562636679708";
    //以vacation为二级域名的价格日历的url
    private static final String CALENDAR_URL_NEW1="http://vacations.lvmama.com/w/product/querySchedule?productId=%s";
    private static final String CALENDAR_URL_NEW2="http://vacations.lvmama.com/tims-web/w/product/queryProductPriceCanlander";
    private Gson gson=new Gson();
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


    /**
     * 爬取以dujia为二级域名的页面的基本信息
     * @param productUrl
     * @param bean
     * @return
     * @throws Exception
     */
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
                    .get(0).getElementsByTag("i").get(0).ownText()
                    .replace("产品编号：","");
            bean.productLink=productUrl;
            bean.taName=document.getElementsByClass("detail_product_num")
                    .get(0).ownText().replace("本产品委托社为","")
                    .replace("，具体的旅游服务和操作由委托社提供。","").trim();
            bean.bPackage.cityName=document.getElementsByClass("start_city_input")
                    .first().ownText().trim();
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

    /**
     * 爬取以dujia为二级域名的页面的基本信息
     * @param productId
     * @param cityCode
     * @param bean
     * @return
     * @throws Exception
     */
    private boolean crawlCalendarOld(String productId,String cityCode,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(CALENDAR_URL_OLD,productId,cityCode),
                null,null,Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            if(responseStr.charAt(0)=='[')
                return true;
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

    /**
     * 爬取以vacation为二级域名的页面的基本信息
     * @param productUrl
     * @param bean
     * @return
     * @throws Exception
     */
    private boolean crawlInfoNew(String productUrl,Bean bean) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            bean.productName=document.getElementsByTag("h2").get(0)
                    .getElementsByTag("em").get(0).ownText()
                    .replaceAll("【.*】","").replaceAll("\\(.*\\)","");
            bean.productLink=productUrl;
            bean.productId=document.getElementsByClass("id-num")
                    .get(0).getElementsByTag("em").get(0).ownText();
            bean.taName=document.getElementsByClass("id-supp")
                    .get(0).getElementsByTag("em").get(0).ownText();
            bean.bPackage.cityName=document.getElementsByClass("lv_topbar js_box")
                    .first().getElementsByClass("lv_city").first().ownText();
            List<List<Bean.Price>> bPriceLists=new ArrayList<>();
            if(document.getElementById("J-departCity")==null){
                return crawlCalendarNew1(bean,bPriceLists)&&crawlPathNew(bean,bPriceLists);
            }
            else{
                String cityCode=document.getElementsByClass("lv_topbar js_box")
                        .first().getElementsByClass("lv_city").first().attr("data-id");
                return crawlCalendarNew2(bean,cityCode,bPriceLists)&&crawlPathNew(bean,bPriceLists);
            }
        }
        else{
            logger.info("failed to request"+productUrl+",code is "+code);
            return false;
        }
    }

    /**
     * 爬取以vacation为二级域名的页面的基本信息
     * @param bean
     * @param bPriceLists
     * @return
     * @throws Exception
     */
    private boolean crawlPathNew(Bean bean,List<List<Bean.Price>> bPriceLists) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(PATH_URL_NEW,bean.productId),
                null,null,Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonArray productTraceInfos=jsonObject.getAsJsonArray("productTraceInfos");
            for(int i=0;i<productTraceInfos.size();i++){
                JsonObject productTraceInfo=productTraceInfos.get(i).getAsJsonObject();
                bean.bPackage.name=productTraceInfo.get("productPackageAlias").getAsString();
                JsonArray strokeDescriptionList=productTraceInfo
                        .getAsJsonArray("strokeDescriptionList");
                StringBuilder builder=new StringBuilder();
                for(int j=0;j<strokeDescriptionList.size();j++){
                    builder.append("D");
                    builder.append(j+1);
                    builder.append(":");
                    builder.append(strokeDescriptionList.get(j).getAsJsonObject()
                            .get("strokeHeading").getAsString());
                }
                bean.bPackage.path=builder.toString();
                bean.priceList.clear();
                bean.priceList.addAll(bPriceLists.get(i));
                //logger.info(gson.toJson(bean));
            }
            if(productTraceInfos.size()==0){
                bean.bPackage.name="跟团游";
                JsonArray strokeDescriptionList=jsonObject.getAsJsonObject("productTraceInfo")
                        .getAsJsonArray("strokeDescriptionList");
                StringBuilder builder=new StringBuilder();
                for(int i=0;i<strokeDescriptionList.size();i++){
                    builder.append("D");
                    builder.append(i+1);
                    builder.append(":");
                    builder.append(strokeDescriptionList.get(i).getAsJsonObject()
                            .get("strokeHeading").getAsString());
                }
                bean.bPackage.path=builder.toString();
                bean.priceList.addAll(bPriceLists.get(0));
            }
            JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
        }
        else{
            logger.info("falied to request"+bean.productLink+",code is "+code);
            return false;
        }
        return true;
    }

    /**
     * 爬取以vacation为二级域名的页面的价格日历
     * 如果城市不可选，则调用CALENDAR_URL_NEW1来获得价格日历
     * @param bean
     * @param bPriceLists
     * @return
     * @throws Exception
     */
    private boolean crawlCalendarNew1(Bean bean,List<List<Bean.Price>> bPriceLists) throws Exception{
        HttpResponse response=HttpClientUtil.doGet(String.format(CALENDAR_URL_NEW1,bean.productId),
                null,null,Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject datePrices=jsonObject.getAsJsonObject("data").getAsJsonObject("datePrices");
            for(Map.Entry<String,JsonElement> datePrice:datePrices.entrySet()){
                JsonArray packageDatePriceDTOS=datePrice.getValue().getAsJsonObject()
                        .getAsJsonArray("packageDatePriceDTOS");
                String date=datePrice.getKey();
                for(int j=0;j<packageDatePriceDTOS.size();j++){
                    if(bPriceLists.size()<j+1){
                        List<Bean.Price> priceList=new ArrayList<>();
                        bPriceLists.add(priceList);
                    }
                    JsonObject dayPriceDTO=packageDatePriceDTOS.get(j).getAsJsonObject()
                            .getAsJsonObject("dayPriceDTO");
                    float price=Float.parseFloat(dayPriceDTO.get("adultPrice").getAsString());
                    Bean.Price bPrice=bean.newPrice(date,price);
                    bPriceLists.get(j).add(bPrice);
                }
            }
            for(int i=0;i<bPriceLists.size();i++){
                List<Bean.Price> priceList=bPriceLists.get(i);
                Collections.sort(priceList,new Comparator<Bean.Price>(){
                    @Override
                    public int compare(Bean.Price o1,Bean.Price o2){
                        return o1.date.compareTo(o2.date);
                    }
                });
            }
            for(int i=0;i<bPriceLists.size();i++){
                List<Bean.Price> priceList=bPriceLists.get(i);
                if(priceList.size()>7){
                    int size=priceList.size();
                    for(int j=0;j<size-7;j++){
                        priceList.remove(priceList.size()-1);
                    }
                }
            }

        }
        else{
            logger.info("failed to request,code is "+code);
            return false;
        }
        return true;
    }

    /**
     * 爬取以vacation为二级域名的页面的价格日历
     * 如果城市可选，则调用CALENDAR_URL_NEW2来获得价格日历
     * @param bean
     * @param cityCode
     * @param bPriceLists
     * @return
     * @throws Exception
     */
    private boolean crawlCalendarNew2(Bean bean,String cityCode,List<List<Bean.Price>> bPriceLists)
            throws Exception{
        List<Bean.Price> priceList=new ArrayList<>();
        bPriceLists.add(priceList);
        Map<String,String> params=new HashMap<>();
        params.put("productId",bean.productId);
        params.put("departCityId",cityCode);
        params.put("lvsign","A4FC0D66964099E82596A971090D972F58FBBC6A");
        params.put("_","1563502084747");
        HttpResponse response=HttpClientUtil.doGet(CALENDAR_URL_NEW2,params,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonObject dayPrices=jsonObject.getAsJsonObject("data").getAsJsonObject("dayPrices");
            for(Map.Entry<String,JsonElement> entry:dayPrices.entrySet()){
                String date=entry.getKey();
                float price=entry.getValue().getAsJsonObject().get("adultPrice").getAsFloat();
                priceList.add(bean.newPrice(date,price));
            }
            for(int i=0;i<bPriceLists.size();i++){
                priceList=bPriceLists.get(i);
                Collections.sort(priceList,new Comparator<Bean.Price>(){
                    @Override
                    public int compare(Bean.Price o1,Bean.Price o2){
                        return o1.date.compareTo(o2.date);
                    }
                });
            }
            for(int i=0;i<bPriceLists.size();i++){
                priceList=bPriceLists.get(i);
                if(priceList.size()>7){
                    int size=priceList.size();
                    for(int j=0;j<size-7;j++){
                        priceList.remove(priceList.size()-1);
                    }
                }
            }
        }
        else{
            logger.info("failed to request "+bean.productLink+",code is "+code);
            return false;
        }
        return true;
    }



    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        try{
            if(taskData.contains("vacations.lvmama.com")){
                if(!crawlInfoNew(taskData,bean)){
                    JedisUtil.lpush(FROM_QUEUE,taskData);
                }
                else{
                    JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
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
        ThreadUtil.waitSecond(2);
    }
    public static void main(String[] args) throws Exception{
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("http://vacations.lvmama.com/w/tour/100096210");
    }
}
