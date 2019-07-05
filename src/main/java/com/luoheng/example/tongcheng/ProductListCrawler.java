package com.luoheng.example.tongcheng;

import com.google.gson.Gson;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
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

public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    private static final String HOST_URL="https://gny.ly.com";
    public static final String FROM_QUEUE="tongcheng_product_task";
    public static final String TO_QUEUE="tongcheng_product_href";
    private static final String[] taskUrls={"https://gny.ly.com/list?src=北京&dest=云南&prop=1",
            "https://gny.ly.com/list?src=北京&dest=云南&prop=5"};
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(ProductListCrawler.class);
    public ProductListCrawler(CrawlerFactory factory){
        super(factory);
        init();
    }

    public ProductListCrawler(CrawlerFactory factory,String name){
        super(factory,name);
        init();
    }

    public ProductListCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
        init();
    }

    private void init(){
        buildTask();
    }

    private void buildTask(){
        try{
            for(String taskUrl:taskUrls){
                Map<String,String> headers=new HashMap<>();
                headers.put("cookie",Core.cookie);
                HttpResponse response=HttpClientUtil.doGet(taskUrl,null,headers);
                int code=response.getStatusLine().getStatusCode();
                if(code==200){
                    String responseStr=EntityUtils.toString(response.getEntity());
                    Document document=Jsoup.parse(responseStr);
                    Elements cityBoxes=document.getElementsByClass("citybox-cont md-nor");
                    int totalCount=Integer.parseInt(document.getElementById("TotalCount").attr("value"));
                    int maxPage=totalCount%20==0?totalCount/20:totalCount/20+1;
                    for(Element cityBox:cityBoxes){
                        Elements as=cityBox.getElementsByTag("a");
                        for(Element a:as){
                            for(int i=1;i<=maxPage;i++){
                                JedisUtil.lpush(FROM_QUEUE,HOST_URL+a.attr("href")+"&start="+i);
                            }
                        }
                    }
                }
                else{
                    logger.error("failed to build tasks");
                    System.exit(-1);
                }
            }
        }catch(IOException e){
            logger.error("failed to build tasks");
            System.exit(-1);
        }
    }

    @Override
    public String getTaskData(){
        String taskData=JedisUtil.rpop(FROM_QUEUE);
        if(taskData==null)
            shouldOver=true;
        return taskData;
    }

    @Override
    public void crawl(String taskData){
        try{
            Map<String,String> headers=new HashMap<>();
            headers.put("cookie",Core.cookie);
            HttpResponse response=HttpClientUtil.doGet(taskData,null,headers);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                Elements as=document.getElementById("line-lsit").getElementsByTag("a");
                for(Element a:as){
                    //logger.info(HOST_URL+a.attr("href"));
                    JedisUtil.lpush(TO_QUEUE,HOST_URL+a.attr("href"));
                }
            }
            else{
                logger.info("failed!code is "+code);
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
        }catch(Exception e){
            JedisUtil.lpush(FROM_QUEUE,taskData);
            Core.saveErrorMsg(taskData);
            if(e instanceof IOException){
                logger.info("IOException");
            }
            else{
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
