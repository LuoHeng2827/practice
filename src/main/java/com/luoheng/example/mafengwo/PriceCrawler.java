package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PriceCrawler extends Crawler{
    public static final String FROM_QUEUE="list_mafengwo_product_info";
    public static final String TO_QUEUE="list_mafengwo_tour_db";
    private static final String DETAIL_URL="http://www.mafengwo.cn/sales/detail/stock/detail";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(PriceCrawler.class);
    public PriceCrawler(CrawlerFactory factory){
        super(factory);
    }

    public PriceCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public PriceCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        String taskData=JedisUtil.rpop(FROM_QUEUE);
        return taskData;
        /*Bean bean=new Bean();
        Bean.Package bPackage=bean.newPackage();
        bPackage.cityName="xxx";
        bPackage.groupId="5575727";
        bPackage.id="35899432";
        bPackage.name="xxx";
        bean.productId="123456";
        bean.productLink="www.xxx.com";
        bean.taName="xxx";
        bean.bPackage=bPackage;
        return gson.toJson(bean);*/
    }

    private boolean getCalendar(Bean bean) throws IOException{
        Map<String,String> params=new HashMap<>();
        Map<String,String> header=new HashMap<>();
        Bean.Package bPackage=bean.bPackage;
        params.put("groupId",bPackage.groupId);
        params.put("skuIds[]",bPackage.id);
        HttpResponse response=HttpClientUtil.doGet(DETAIL_URL,params,header,true,number);
        int responseCode=response.getStatusLine().getStatusCode();
        if(responseCode==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            if(jsonObject.get("data") instanceof JsonNull)
                return true;
            JsonArray sku=jsonObject.getAsJsonObject("data")
                    .getAsJsonArray("sku");
            if(sku.size()>1)
                logger.info("unexpect error,sku.size is "+sku.size());
            JsonObject skuItem=sku.get(0).getAsJsonObject();
            JsonArray calendar=skuItem.get("calendar").getAsJsonArray();
            int saveDay=Math.min(7,calendar.size()/2);
            Map<String,String> calendarMap=new HashMap<>();
            for(int i=0;i<calendar.size()&&calendarMap.size()<saveDay;i++){
                JsonObject calendarItem=calendar.get(i).getAsJsonObject();
                String dateStr=calendarItem.get("date").getAsString();
                String priceStr=calendarItem.get("price").getAsString();
                if(calendarMap.containsKey(dateStr))
                    continue;
                calendarMap.put(dateStr,priceStr);
                Bean.Price price=bean.newPrice();
                price.price=Float.parseFloat(priceStr);
                price.date=dateStr;
                bean.priceList.add(price);
            }
            saveData(bean);
            //logger.info(gson.toJson(bean));
        }
        else{
            logger.info("failed to request,code is "+responseCode);
            return false;
        }
        return true;
    }

    private void saveFailureTask(String taskData){
        logger.info(taskData+" push to queue");
        JedisUtil.lpush(FROM_QUEUE,taskData);
    }

    private void saveData(Bean bean){
        JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
    }

    @Override
    public void crawl(String taskData){
        Bean bean=gson.fromJson(taskData,Bean.class);
        try{
            if(!getCalendar(bean)){
                saveFailureTask(taskData);
                return;
            }
        }catch(IOException e){
            saveFailureTask(taskData);
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        PriceCrawler crawler=new PriceCrawler(null);
        crawler.start();
    }
}
