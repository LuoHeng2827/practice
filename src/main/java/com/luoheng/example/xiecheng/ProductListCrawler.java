package com.luoheng.example.xiecheng;

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

public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    private static final String[] TARGET_URLS={"https://vacations.ctrip.com/tours/d-yunnan-100007/around/p%s"
            ,"https://vacations.ctrip.com/tours/d-yunnan-100007/grouptravel/p%s"};
    public static final String FROM_QUEUE="list_xiecheng_task";
    public static final String TO_QUEUE="list_xiecheng_href";
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
        for(String url:TARGET_URLS){
            try{
                HttpResponse response=HttpClientUtil.doGet(String.format(url,1),null,null);
                int code=response.getStatusLine().getStatusCode();
                if(code==200){
                    String responseStr=EntityUtils.toString(response.getEntity());
                    Document document=Jsoup.parse(responseStr);
                    Element _pg=document.getElementById("_pg");
                    Elements as=_pg.getElementsByTag("a");
                    int maxPage=Integer.parseInt(as.get(as.size()-2).text());
                    for(int i=0;i<maxPage;i++){
                        JedisUtil.lpush(TO_QUEUE,String.format(url,i+1));
                    }
                }
                else{
                    logger.info("error to build task,code is "+code);
                    System.exit(-1);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }

    @Override
    public void crawl(String taskData){
        try{
            HttpResponse response=HttpClientUtil.doGet(taskData,null,null);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                Elements products=document.getElementsByClass("main_mod product_box flag_product ");
                for(Element product:products){
                    String href=product.getElementsByClass("product_pic")
                            .get(0).getElementsByTag("a").get(0).attr("href");
                    JedisUtil.lpush(TO_QUEUE,href);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
