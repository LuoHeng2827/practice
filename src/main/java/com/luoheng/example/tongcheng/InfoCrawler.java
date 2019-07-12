package com.luoheng.example.tongcheng;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoCrawler extends Crawler{
    //价格日历链接
    private static final String CALENDAR_URL="https://gny.ly.com/fetch/index/detail/getCalendarList";
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
        headers.put("Cookie",Core.cookie);
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,headers);
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
            return crawlCalendar(cityName,cityCode,bean);
        }
        else{
            logger.info("failed to request,code is "+code);
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

    //爬取网站的价格日历
    private boolean crawlCalendar(String cityName,String cityCode,Bean bean) throws Exception{
        Map<String,String> headers=new HashMap<>();
        List<NameValuePair> formList=new ArrayList<>();
        headers.put("Cookie",Core.cookie);
        headers.put("Content-type","application/x-www-form-urlencoded");
        formList.add(new BasicNameValuePair("ProductId",bean.productId));
        formList.add(new BasicNameValuePair("depId",cityCode));
        formList.add(new BasicNameValuePair("DepCityName",cityName));
        formList.add(new BasicNameValuePair("AdultNum","2"));
        formList.add(new BasicNameValuePair("ChildNum","0"));
        UrlEncodedFormEntity entity=new UrlEncodedFormEntity(formList,Charset.forName("UTF-8"));
        HttpResponse response=HttpClientUtil.doPost(CALENDAR_URL,null,headers,entity);
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
            logger.info("failed to request,code is "+code);
            return false;
        }
        return true;
    }

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
                Core.saveErrorMsg(e.getMessage());
                e.printStackTrace();
            }
            else{
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("https://gny.ly.com/line/t1j3p1093495c317.html");
    }
}
