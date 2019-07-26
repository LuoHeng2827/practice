package com.luoheng.example.tuniu;

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

//爬取产品列表的产品链接
public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    public static final String FROM_QUEUE="tuniu_task";
    public static final String TO_QUEUE="tuniu_product_link";
    public static final String LIST_URL="http://www.tuniu.com/g3300/tours-wh-0/%s";
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
        JedisUtil.lpush(FROM_QUEUE,String.format(LIST_URL,"1"));
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
            HttpResponse response=HttpClientUtil.doGet(taskData,null,null,
                    Boolean.parseBoolean(PropertiesUtil.getValue("proxy.use")),number);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                if(document.getElementsByClass("thebox clearfix zizhubox").size()<=0)
                    return;
                Element pageElement=document.getElementsByClass("page-bottom").first();
                Elements pageA=pageElement.getElementsByTag("a");
                int maxPage=Integer.parseInt(pageA.get(pageA.size()-2).ownText());
                for(int i=2;i<maxPage;i++){
                    JedisUtil.lpush(FROM_QUEUE,String.format(LIST_URL,i+""));
                }
                Elements products=document.getElementsByClass("thebox clearfix zizhubox").first()
                        .getElementsByTag("li");
                for(Element product:products){
                    String href=product.getElementsByClass("theinfo").first()
                            .getElementsByTag("a").first().attr("href");
                    JedisUtil.lpush(TO_QUEUE,"http:"+href);
                }
            }
            else{
                logger.info("failed to request "+taskData+",code is "+code);
                JedisUtil.lpush(taskData);
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
        ThreadUtil.waitMillis(1000);
    }
}
