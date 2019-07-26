package com.luoheng.example.tongcheng;

import com.google.gson.Gson;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.BloomFilter.BFUtil;
import com.luoheng.example.util.BloomFilter.BfConfiguration;
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

//爬取产品列表的产品链接
public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    private static final String HOST_URL="https://gny.ly.com";
    public static final String FROM_QUEUE="tongcheng_product_task";
    public static final String TO_QUEUE="tongcheng_product_href";

    //产品列表链接
    /*private static final String[] taskUrls={"https://m.ly.com/guoneiyou/list?src=%E5%8C%97%E4%BA%AC&dest=%E4%BA%91%E5%8D%97&prop=1",
            "https://m.ly.com/guoneiyou/list?src=%E5%8C%97%E4%BA%AC&dest=%E4%BA%91%E5%8D%97&prop=5"};*/
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

    /**
     * 遍历城市列表，构建任务
     */
    public void buildTask(){
        try{
            for(String taskUrl:taskUrls){
                Map<String,String> headers=new HashMap<>();
                headers.put("cookie",Core.cookies[0]);
                headers.put("Accept","*/*");
                headers.put("Host","gny.ly.com");
                headers.put("Connection","keep-alive");
                headers.put("User-Agent",Core.randomUA(1,10));
                HttpResponse response=HttpClientUtil.doGet(taskUrl,null,headers,
                        Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),29);
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
                                //logger.info(a.attr("href"));
                                JedisUtil.lpush(FROM_QUEUE,HOST_URL+a.attr("href")+"&start="+i);
                            }
                        }
                    }
                }
                else{
                    logger.error("failed to build tasks,code is "+code);
                    System.exit(-1);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
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

    //获得产品链接
    @Override
    public void crawl(String taskData){
        try{
            Map<String,String> headers=new HashMap<>();
            headers.put("cookie",Core.cookies[0]);
            headers.put("User-Agent",Core.randomUA(1,10));
            HttpResponse response=HttpClientUtil.doGet(taskData,null,headers,
                    Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),16);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                if(document.getElementById("line-lsit")==null)
                    return;
                Elements as=document.getElementById("line-lsit").getElementsByTag("a");
                for(Element a:as){
                    String href=HOST_URL+a.attr("href");
                    if(!Core.isUpdatePrice){
                        if(BFUtil.isExist(href))
                            continue;
                        JedisUtil.lpush(TO_QUEUE,href);
                    }
                    else{
                        JedisUtil.lpush(TO_QUEUE,href);
                    }
                }
            }
            else{
                logger.info("failed!code is "+code);
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
        ThreadUtil.waitMillis(200);
    }
    public static void main(String[] args) throws Exception{
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
