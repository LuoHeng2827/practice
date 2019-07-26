package com.luoheng.example.tongcheng;

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
    //价格日历链接
    private static final String CALENDAR_URL="https://m.ly.com/guoneiyou/calendar?";
    public static final String FROM_QUEUE="tongcheng_product_href";
    public static final String TO_QUEUE="tongcheng_product_db";
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

    //爬取产品的基本信息
    private boolean crawlInfo(String productUrl,Bean bean) throws Exception{
        Map<String,String> headers=new HashMap<>();
        headers.put("cookie:",Core.cookies[number+1]);
        headers.put("User-Agent",Core.randomUA(1,number));
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,headers
                ,Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            bean.productName=document.getElementById("mainTitle").attr("value");
            if(document.getElementsByClass("specialLineLabel").size()>0)
                bean.bPackage.name="同程专线";
            else
                bean.bPackage.name="跟团游";
            bean.productId=document.getElementById("HidLineid").attr("value");
            if(document.getElementsByClass("other_city J_otherC").size()>0)
                bean.musterType=Bean.SRC_MUSTER;
            else
                bean.musterType=Bean.DES_MUSTER;
            String cityName=document.getElementById("StartCityName").attr("value");
            String cityCode=document.getElementById("StartCityId").attr("value");
            bean.musterPlace=cityName;
            bean.bPackage.cityName=cityName;
            crawlPath(document.getElementsByClass("excel-wrap none J-Simple").get(0),bean);
            return crawlCalendar(cityCode,bean);
        }
        else{
            logger.info("failed to request "+productUrl+",code is "+code);
            return false;
        }
    }

    //爬取产品的路线信息
    private void crawlPath(Element div,Bean bean){
        Elements trs=div.getElementsByTag("tbody").get(0).getElementsByTag("tr");
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<trs.size();i++){
            builder.append("D");
            builder.append(i+1);
            builder.append(":");
            Element tr=trs.get(i);
            Element td=tr.getElementsByClass("first").get(0);
            for(Element span:td.getElementsByTag("span")){
                builder.append(span.ownText());
                builder.append("-");
            }
            builder.deleteCharAt(builder.length()-1);
        }
        bean.bPackage.path=builder.toString();
    }

    private boolean crawlCalendar(String cityCode,Bean bean) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("lineid",bean.productId);
        params.put("cityId",cityCode);
        params.put("dk","");
        params.put("ratio","2");
        headers.put("User-Agent",Core.randomUA(1,number));
        headers.put("cookie:",Core.cookies[number+1]);
        HttpResponse response=HttpClientUtil.doGet(CALENDAR_URL,params,headers,
                Boolean.parseBoolean(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            String jsonStr=null;
            Elements scripts=document.getElementsByTag("script");
            for(Element script:scripts){
                if(script.data().contains("var priceList")){
                    jsonStr=script.data().replace("var priceList = ","").trim();
                    jsonStr=jsonStr.substring(0,jsonStr.length()-1);
                    JsonObject jsonObject=gson.fromJson(jsonStr,JsonObject.class);
                    JsonArray PriceCalendarQueryList=jsonObject
                            .getAsJsonArray("PriceCalendarQueryList");
                    int day=Math.min(7,PriceCalendarQueryList.size());
                    for(int i=0;i<day;i++){
                        JsonObject item=PriceCalendarQueryList.get(i).getAsJsonObject();
                        String date=item.get("StartDateFormat").getAsString();
                        float price=item.get("AdultSalePrice").getAsFloat();
                        Bean.Price bPrice=bean.newPrice(date,price);
                        bean.priceList.add(bPrice);
                    }
                    break;
                }
            }
        }
        else{
            logger.info("failed to request "+HttpClientUtil.generateGetParams(CALENDAR_URL,params)
                    +",code is "+code);
            return false;
        }
        return true;
    }

    //爬取网站的价格日历
    /*private boolean crawlCalendar(String cityName,String cityCode,Bean bean) throws Exception{
        Map<String,String> headers=new HashMap<>();
        List<NameValuePair> formList=new ArrayList<>();
        headers.put("Content-type","application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("User-Agent",Core.UA);
        formList.add(new BasicNameValuePair("ProductId",bean.productId));
        formList.add(new BasicNameValuePair("depId",cityCode));
        formList.add(new BasicNameValuePair("DepCityName",cityName));
        formList.add(new BasicNameValuePair("AdultNum","2"));
        formList.add(new BasicNameValuePair("ChildNum","0"));
        UrlEncodedFormEntity entity=new UrlEncodedFormEntity(formList,Charset.forName("UTF-8"));
        HttpResponse response=HttpClientUtil.doPost(CALENDAR_URL,null,headers,entity,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonArray priceList=jsonObject.getAsJsonObject("calInfo").getAsJsonArray("priceList");
            int day=Math.min(7,priceList.size());
            for(int i=0;i<day;i++){
                JsonObject priceItem=priceList.get(i).getAsJsonObject();
                String date=priceItem.get("Date").getAsString();
                String price=priceItem.get("Price").getAsString();
                Bean.Price bPrice=bean.newPrice(date,Float.parseFloat(price));
                bean.priceList.add(bPrice);
            }
        }
        else{
            logger.info("failed to request"+bean.productLink+",code is "+code);
            return false;
        }
        logger.info(bean.priceList.toString());
        return true;
    }*/

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        bean.productLink=taskData;
        try{
            if(!crawlInfo(taskData,bean)){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
            else{
                //logger.info(gson.toJson(bean));
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
            }
        }catch(Exception e){
            JedisUtil.lpush(FROM_QUEUE,taskData);
            if(!(e instanceof IOException)){
                Core.saveErrorMsg(taskData+"\n"+ExceptionUtil.getTotal(e));
                e.printStackTrace();
            }
            else{
                e.printStackTrace();
            }
        }
        ThreadUtil.waitMillis(500);
    }
    public static void main(String[] args) throws Exception{
        InfoCrawler crawler=new InfoCrawler(null);
        Bean bean=new Bean();
        bean.productId="1071407";
        crawler.crawlCalendar("324",bean);
    }
}
