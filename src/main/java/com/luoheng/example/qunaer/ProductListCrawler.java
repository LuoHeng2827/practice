package com.luoheng.example.qunaer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获得产品的链接
 */
public class ProductListCrawler extends Crawler{
    public static boolean shouldOver=false;
    public static final String FROM_QUEUE="qunaer_task";
    public static final String TO_QUEUE="qunaer_product_href";
    //产品列表的URL
    private static final String PRODUCT_LIST_URL="https://dujia.qunar.com/golfz/routeList/adaptors/pcTop";
    //城市列表的URL
    private static final String CITY_LIST_URL="https://dujia.qunar.com/golfz/searchbox/departuresNew?callback=jQuery172014593998509217254_1562655723728&modules=domestic%2Cabroad&_=1562663552687";
    private static final String[] DES_CITY_NAME={"丽江","玉龙雪山","昆明","大理","香格里拉","西双版纳","迪庆","束河","洋人街","双廊","泸沽湖"};
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

    //生成产品列表请求的参数
    private Map<String,String> buildListParams(String lm,String cityName,String q){
        Map<String,String> params=new HashMap<>();
        params.put("isTouch","0");
        params.put("t","all");
        params.put("extendFunction","跟团游");
        params.put("o","pop-desc");
        params.put("lm",lm);
        params.put("fhLimit","0,60");
        params.put("q",q);
        params.put("d",cityName);
        params.put("s","all");
        params.put("qs_ts","1562654042468");
        params.put("tm","gn02_origin");
        params.put("sourcepage","list");
        params.put("userResident",cityName);
        params.put("random","-1");
        params.put("aroundWeight","1");
        params.put("qssrc","eyJ0cyI6IjE1NjI3MjI0OTMzODciLCJzcmMiOiJhbGwuZW52YSIsImFjdCI6InNjcm9sbCIsInJhbmRvbSI6IjcwNDQwMCJ9");
        params.put("m","l,bookingInfo,browsingInfo,lm");
        params.put("displayStatus","pc");
        params.put("ddf","true");
        params.put("userId","1");
        params.put("hlFields","title");
        params.put("gpscity",cityName);
        params.put("lines6To10","0");
        return params;
    }

    private void init(){
        buildTask();
    }

    //遍历城市列表，创建任务
    private void buildTask(){
        List<String> cityNameList=getCityNameList();
        for(String desCityName:DES_CITY_NAME){
            for(String cityName:cityNameList){
                JedisUtil.lpush(FROM_QUEUE,
                        HttpClientUtil.generateGetParams(PRODUCT_LIST_URL,buildListParams("0,60",cityName,desCityName)));
            }
        }
        long len=JedisUtil.llen(FROM_QUEUE);
        List<String> verifyList=JedisUtil.lrange(FROM_QUEUE,0,len-1);
        if(verifyList.size()!=cityNameList.size()*DES_CITY_NAME.length){
            logger.info("failed to build task!");
            System.exit(-1);
        }
    }

    //获得城市名字列表
    private List<String> getCityNameList(){
        List<String> cityNameList=new ArrayList<>();
        try{
            HttpResponse response=HttpClientUtil.doGet(CITY_LIST_URL,null,null);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                responseStr=responseStr.replace("jQuery172014593998509217254_1562655723728(","");
                responseStr=responseStr.substring(0,responseStr.length()-2);
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject domestic=jsonObject.getAsJsonObject("data").getAsJsonObject("domestic");
                for(Map.Entry<String,JsonElement> entry:domestic.entrySet()){
                    JsonArray domesticItem=entry.getValue().getAsJsonArray();
                    for(int i=0;i<domesticItem.size();i++){
                        String cityName=domesticItem.get(i).getAsJsonObject().get("name").getAsString();
                        cityNameList.add(cityName);
                    }
                }
            }
            else{
                logger.info("failed to get city list");
                System.exit(-1);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return cityNameList;
    }

    @Override
    public String getTaskData(){
        String taskData=JedisUtil.rpop(FROM_QUEUE);
        if(taskData==null){
            shouldOver=true;
            overThis();
        }
        return taskData;
    }

    @Override
    public void crawl(String taskData){
        try{
            Map<String,String> headers=new HashMap<>();
            HttpResponse response=HttpClientUtil.doGet(taskData,null,headers);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                JsonObject list=jsonObject.getAsJsonObject("data").getAsJsonObject("list");
                int productNum=list.get("numFound").getAsInt();
                JsonArray results=list.getAsJsonArray("results");
                for(int i=0;i<results.size();i++){
                    JedisUtil.lpush(TO_QUEUE,"https:"+results.get(i).getAsJsonObject().get("url").getAsString());
                }
                for(int i=60;i<productNum;i+=60){
                    JedisUtil.lpush(FROM_QUEUE,taskData.replace("0,60",""+i+",60"));
                }
            }
            else{
                logger.info("failed to request "+taskData+"code is "+code);
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
        }catch(Exception e){
            if(e instanceof IOException){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
            else{
                Core.saveErrorMsg(ExceptionUtil.getTotal(e));
            }
        }
    }
    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
