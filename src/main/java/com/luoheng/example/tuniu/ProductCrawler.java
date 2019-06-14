package com.luoheng.example.tuniu;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 跟团游和自由行产品爬虫
 */
public class ProductCrawler extends Crawler {
    private static final String TARGET_URL="https://m.tuniu.com/travel/mapi/search/getSearchList";
    private static final String TO_TOUR_QUEUE="list_tuniu_tour_product_id";
    private static final String TO_PKG_QUEUE="list_tuniu_pkg_product_id";
    public static final int TYPE_TOUR=0;
    public static final int TYPE_PKG=1;
    private Queue<String> taskQueue;
    private Gson gson;
    private int type;
    private Logger logger=LogManager.getLogger(ProductCrawler.class);
    public ProductCrawler(CrawlerFactory factory,int type) {
        super(factory);
        this.type=type;
    }

    public ProductCrawler(CrawlerFactory factory,int type,String name) {
        super(factory,name);
        this.type=type;
    }

    @Override
    public void init() {
        super.init();
        gson=new Gson();
        taskQueue=buildTask();
    }

    private Queue<String> buildTask(){
        Queue<String> taskQueue=new ConcurrentLinkedQueue<>();
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("limit","20");
        params.put("paramType","1");
        params.put("keyword","云南");
        if(type==TYPE_TOUR)
            params.put("tabKey","tours");
        else
            params.put("tabKey","pkg");
        params.put("pageNum","1");
        params.put("newSaleChannels","[]");
        headers.put("User-Agent","Chrome/74.0.3729.169 Mobile");
        headers.put("Referer","https://m.tuniu.com/h5/search/index");
        try{
            Response response=HttpUtil.doGet(TARGET_URL,params,headers);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                int pageCount=data.get("pageCount").getAsInt();

                for(int i=0;i<pageCount;i++){
                    JsonObject task=new JsonObject();
                    task.addProperty("url",TARGET_URL);
                    params.put("pageNum",i+1+"");
                    task.addProperty("params",gson.toJson(params));
                    task.addProperty("headers",gson.toJson(headers));
                    taskQueue.offer(task.toString());
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return taskQueue;
    }

    @Override
    public String getTaskData() {
        String taskData=taskQueue.poll();
        if(taskData==null){
            over();
            getFactory().notifyOver();
        }
        return taskData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void crawl(String taskData) {
        JsonObject task=gson.fromJson(taskData,JsonObject.class);
        try{
            String targetUrl=task.get("url").getAsString();
            Map<String,String> params=gson.fromJson(task.get("params").getAsString(),HashMap.class);
            Map<String,String> headers=gson.fromJson(task.get("headers").getAsString(),HashMap.class);
            Jedis jedis=JedisUtil.getResource();
            Response response=HttpUtil.doGet(targetUrl,params,headers);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                if(data.get("list") instanceof JsonNull){
                    return;
                }
                JsonArray list=data.getAsJsonArray("list");
                for(int i=0;i<list.size();i++){
                    long productId=list.get(i).getAsJsonObject().get("productId").getAsLong();
                    if(type==TYPE_TOUR){
                        jedis.rpush(TO_TOUR_QUEUE,productId+"");
                    }
                    else
                        jedis.rpush(TO_PKG_QUEUE,productId+"");

                }
            }
            else{
                logger.info("failed to request "+response.request().url());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        ProductCrawler crawler=new ProductCrawler(null,ProductCrawler.TYPE_TOUR);
        crawler.start();
    }
}
