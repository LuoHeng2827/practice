package com.luoheng.example.tongcheng;

import com.google.gson.Gson;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    private static final String HOST_URL="https://gny.ly.com";
    public static final String FROM_QUEUE="tongcheng_product_task";
    public static final String TO_QUEUE="tongcheng_product_href";
    //产品列表链接
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

    //获得访问需要的Cookie
    private void crawlCookie(){
        Map<String,String> headers=new HashMap<>();
        headers.put("Cookie","dj-meta=%257B%2522ttf%2522%3A%2522100000001001110101101000101111000011010101110000101%257C10001010100110010010001%2522%2C%2522tz%2522%3A-480%2C%2522au%2522%3A%252248000_2_1_0_2_explicit_speakers%2522%2C%2522gp%2522%3A%2522Google%2520Inc.%40ANGLE%2520(NVIDIA%2520GeForce%2520GT%2520705%2520Direct3D11%2520vs_5_0%2520ps_5_0)%2522%2C%2522cv%2522%3A%252275ef4ff7dba805b5200bfb170dd6ceaed666c140%2522%2C%2522pls%2522%3A%2522Chrome%2520PDF%2520PluginChrome%2520PDF%2520ViewerNative%2520Client%2522%2C%2522hd%2522%3A%2522zh-CN_zh_4%2522%2C%2522sc%2522%3A%2522900_1600_24_1%2522%2C%2522ua%2522%3A%2522Mozilla%2F5.0%2520(Macintosh%3B%2520Intel%2520Mac%2520OS%2520X%252010_9_3)%2520AppleWebKit%2F537.75.14%2520(KHTML%2C%2520like%2520Gecko)%2520Version%2F7.0.3%2520Safari%2F7046A194A%2522%2C%2522ft%2522%3A%2522a0d3a5ae529422a5960d9975b42f14e673c6a992%2522%2C%2522lg%2522%3A%2522b0b257f74eb224153921aa5e43565375f0cebd8c%2522%257D");
        try{
            List<Cookie> cookieList=HttpClientUtil.getDoGetCookie(taskUrls[0],null,headers);
            for(Cookie cookie:cookieList){
                if(cookie.getName().equals("dj-po")){
                    Core.cookie=cookie.getName()+"="+cookie.getValue();
                }
            }
        }catch(IOException e){
            logger.info("failed to crawl cookie");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void init(){
        crawlCookie();
        buildTask();
    }

    //遍历城市列表，构建任务
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

    //获得产品链接
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
                if(document.getElementById("line-lsit")==null)
                    return;
                Elements as=document.getElementById("line-lsit").getElementsByTag("a");
                for(Element a:as){
                    JedisUtil.lpush(TO_QUEUE,HOST_URL+a.attr("href"));
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
    }
    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
