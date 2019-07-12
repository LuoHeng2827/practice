package com.luoheng.example._tuniu;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.http.OkHttpUtil;
import com.luoheng.example.util.redis.JedisUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 跟团游和自由行产品爬虫
 */
public class ProductListCrawler extends Crawler {
    private static final String TARGET_URL="https://m.tuniu.com/travel/mapi/search/getSearchList";
    private static final String TO_TOUR_QUEUE="list_tuniu_product_id";
    private Queue<String> taskQueue;
    private Gson gson;
    private Logger logger=LogManager.getLogger(ProductListCrawler.class);

    public ProductListCrawler(CrawlerFactory factory){
        super(factory);
        init();
    }


    public ProductListCrawler(CrawlerFactory factory,String name) {
        super(factory,name);
        init();
    }

    public void init() {
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
        params.put("tabKey","tours");
        params.put("pageNum","1");
        params.put("newSaleChannels","[]");
        headers.put("User-Agent","Chrome/74.0.3729.169 Mobile");
        headers.put("Referer","https://m.tuniu.com/h5/search/index");
        try{
            Response response=OkHttpUtil.doGet(TARGET_URL,params,headers);
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
        return taskQueue.poll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void crawl(String taskData) {
        JsonObject task=gson.fromJson(taskData,JsonObject.class);
        Jedis jedis=JedisUtil.getResource();
        try{
            String targetUrl=task.get("url").getAsString();
            Map<String,String> params=gson.fromJson(task.get("params").getAsString(),HashMap.class);
            Map<String,String> headers=gson.fromJson(task.get("headers").getAsString(),HashMap.class);
            Response response=OkHttpUtil.doGet(targetUrl,params,headers);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject data=jsonObject.getAsJsonObject("data");
                if(data.get("list") instanceof JsonNull){
                    return;
                }
                JsonArray list=data.getAsJsonArray("list");
                for(int i=0;i<list.size();i++){
                    String productId=list.get(i).getAsJsonObject().get("productId").getAsString();
                    jedis.rpush(TO_TOUR_QUEUE,productId);
                }
            }
            else{
                logger.info("failed to request "+response.request().url());
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            jedis.close();
        }
    }

    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
