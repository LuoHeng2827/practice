package com.luoheng.example.lvmama;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    public static final String FROM_QUEUE="lvmama_task";
    public static final String TO_QUEUE="lvmama_product_href";
    private static final String CITY_LIST_URL="https://login.lvmama.com/seo_api/departureList/getDepartVo.do?channel=zhuzhan&jsoncallback=recive&callback=recive&_=1562315319265";
    //占位符为出发城市编号
    private static final String HTML_URL="http://s.lvmama.com/group/H%sK440300?keyword=云南&k=0";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(ProductListCrawler.class);
    public ProductListCrawler(CrawlerFactory factory){
        super(factory);
    }

    public ProductListCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public ProductListCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    private void buildTask(){
        try{
            HttpResponse response=HttpClientUtil.doGet(CITY_LIST_URL,null,null);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Pattern pattern=Pattern.compile("recive\\\\((.*)\\\\)");
                Matcher matcher=pattern.matcher(responseStr);
                if(matcher.matches()){
                    JsonObject jsonObject=gson.fromJson(matcher.group(1),JsonObject.class);
                    for(Map.Entry<String,JsonElement> entry:jsonObject.entrySet()){
                        if(entry.getKey().equals("hot"))
                            continue;
                        JsonArray cityArray=entry.getValue().getAsJsonArray();
                        for(int i=0;i<cityArray.size();i++){
                            JsonObject cityObject=cityArray.get(i).getAsJsonObject();
                            String cityCode=cityObject.get("districtId").getAsString();
                            JedisUtil.lpush(TO_QUEUE,String.format(HTML_URL,cityCode));
                        }
                    }

                }
                else{
                    logger.info("error,can not get city list");
                    System.exit(-1);
                }
            }
            else{
                logger.info("failed to request,code is "+code);
                System.exit(-1);
            }
        }catch(Exception e){
            if(e instanceof IOException){

            }
            else{

            }
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

    }
}
