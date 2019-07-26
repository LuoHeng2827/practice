package com.luoheng.example.tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.BloomFilter.BFUtil;
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

public class AllHrefCrawler extends Crawler{
    public static final String FROM_QUEUE="tuniu_product_link";
    public static final String TO_QUEUE="tuniu_product_all_link";
    private Logger logger=LogManager.getLogger(AllHrefCrawler.class);
    private Gson gson=new Gson();
    public AllHrefCrawler(CrawlerFactory factory){
        super(factory);
    }

    public AllHrefCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public AllHrefCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }


    @Override
    public void crawl(String taskData){
        try{
            HttpResponse response=HttpClientUtil.doGet(taskData,null,null,
                    Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                Elements scripts=document.getElementsByTag("script");
                for(Element script:scripts){
                    if(script.data().contains("window.pageData = {")){
                        String scriptContent=script.data().trim();
                        scriptContent=scriptContent.replace("window.pageData = ","")
                        .replace("window.siteHost = \"http://www.tuniu.com/\";","")
                        .trim();
                        scriptContent=scriptContent.substring(0,scriptContent.length()-1);
                        JsonObject jsonObject=gson.fromJson(scriptContent,JsonObject.class);
                        JsonArray departCityList=jsonObject.getAsJsonArray("departCityList");
                        for(int i=0;i<departCityList.size();i++){
                            JsonObject object=departCityList.get(i).getAsJsonObject();
                            String cityCode=object.get("code").getAsString();
                            //logger.info(taskData+"/"+cityCode+"/"+cityCode+"/"+cityCode);
                            String href=taskData+"/"+cityCode+"/"+cityCode+"/"+cityCode;
                            if(Core.isUpdatePrice){
                                JedisUtil.lpush(TO_QUEUE,href);
                            }
                            else{
                                if(!BFUtil.isExist(href))
                                    JedisUtil.lpush(TO_QUEUE,href);
                            }
                        }
                    }
                }
            }
            else{
                logger.info("failed to request "+taskData+",code is "+code);
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
        ThreadUtil.waitMillis(1000);
    }
    public static void main(String[] args){
        AllHrefCrawler crawler=new AllHrefCrawler(null);
        crawler.crawl("http://www.tuniu.com/tour/210443228");
    }
}
