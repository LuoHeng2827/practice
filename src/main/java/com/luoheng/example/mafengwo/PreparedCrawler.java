package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.http.OkHttpUtil;
import com.luoheng.example.util.redis.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;

public class PreparedCrawler extends Crawler{
    private static final long MAX_FREE_TIME=9000;
    private static final long FREE_TIME_INTERVAL=3000;
    private static final String TARGET_URL="http://www.mafengwo.cn/sales/ajax_2017.php";
    private static final String HOST_URL="http://www.mafengwo.cn";
    public static final String TO_QUEUE="list_mafengwo_product_id";
    public static final String TASK_QUEUE="list_mafengwo_product_id_task";
    private static String[] destinationCityCodes={"M10186","M10807","M10482","M10121","M12711","M10487"};
    private Logger logger=LogManager.getLogger(PreparedCrawler.class);
    private Gson gson;
    private List<String> repeatFilter=new ArrayList<>();
    private long freeTime;
    public PreparedCrawler(CrawlerFactory factory){
        super(factory);
    }

    public PreparedCrawler(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public PreparedCrawler(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    @Override
    public void init(){
        super.init();
        gson=new Gson();
        buildTaskData();
    }

    private void buildTaskData(){
        Jedis jedis=JedisUtil.getResource();
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        headers.put("Accept","*/*");
        params.put("act","GetContentList");
        params.put("from","0");
        params.put("group","4");
        params.put("sort","smart");
        params.put("sort_type","desc");
        params.put("limit","20");
        for(int i=0;i<destinationCityCodes.length;i++){
            params.put("to",destinationCityCodes[i]);
            params.put("page","1");
            try{
                Response response=OkHttpUtil.doGet(TARGET_URL,params,headers);
                if(response.code()==200){
                    String responseStr=response.body().string();
                    JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                    JsonObject msg=jsonObject.get("msg").getAsJsonObject();
                    int totalItem=msg.get("total").getAsInt();
                    int totalPage=totalItem%20==0?totalItem/20:totalItem/20+1;
                    for(int j=0;j<totalPage;j++){
                        JsonObject taskData=new JsonObject();
                        params.put("page",j+1+"");
                        taskData.addProperty("url",TARGET_URL);
                        taskData.addProperty("params",gson.toJson(params));
                        taskData.addProperty("headers",gson.toJson(headers));
                        jedis.lpush(TASK_QUEUE,taskData.toString());
                    }
                }
                else{
                    logger.info("failed to request url "+response.request().url());
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally{
                jedis.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void crawProductUrl(String taskData) throws IOException{
        Jedis jedis=JedisUtil.getResource();
        JsonObject taskDataObject=gson.fromJson(taskData,JsonObject.class);
        String url=taskDataObject.get("url").getAsString();
        Map<String,String> params=gson.fromJson(taskDataObject.get("params").getAsString(),HashMap.class);
        Map<String,String> headers=gson.fromJson(taskDataObject.get("headers").getAsString(),HashMap.class);
        Response response=OkHttpUtil.doGet(url,params,headers);
        if(response.code()==200){
            String responseStr=response.body().string();
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            String html=jsonObject.get("html").getAsString();
            if(html.equals(""))
                return;
            Document document=Jsoup.parse(html);
            Elements elements=document.getElementsByTag("a");
            for(Element element:elements){
                String href=element.attr("href");
                String productId=href.substring(href.lastIndexOf("/")+1,href.lastIndexOf("."));
                if(!repeatFilter.contains(productId)){
                    repeatFilter.add(productId);
                    jedis.lpush(TO_QUEUE,productId);
                }
            }
        }
        else{
            jedis.lpush(TASK_QUEUE,taskData);
            logger.info("failed to request url "+response.request().url());
        }
        jedis.close();
    }

    @Override
    public String getTaskData(){
        Jedis jedis=JedisUtil.getResource();
        String taskData=jedis.rpop(TASK_QUEUE);
        if(taskData==null){
            while(true){
                freeTime+=FREE_TIME_INTERVAL;
                ThreadUtil.waitMillis(FREE_TIME_INTERVAL);
                if(freeTime>=MAX_FREE_TIME){
                    getFactory().notifyOver();
                    break;
                }
            }
        }
        else{
            freeTime=0;
        }
        return taskData;
    }

    @Override
    public void crawl(String taskData){
        try{
            crawProductUrl(taskData);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        PreparedCrawler crawler=new PreparedCrawler(null);
        crawler.start();
    }
}
