package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.PropertiesUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PreparedCrawler extends Crawler{
    //获得产品列表的链接
    private static final String PRODUCT_LIST_URL="http://www.mafengwo.cn/sales/ajax_2017.php";
    public static final String TO_QUEUE="list_mafengwo_product_id";
    public static final String FROM_QUEUE="list_mafengwo_product_id_task";
    //目的地编号
    private static String[] destinationCityCodes={"M10186","M10807","M10482","M10121","M12711","M10487"};
    public static boolean shouldOver=false;
    private Logger logger=LogManager.getLogger(PreparedCrawler.class);
    private Gson gson=new Gson();
    public PreparedCrawler(CrawlerFactory factory){
        super(factory);
        init();
    }

    public PreparedCrawler(CrawlerFactory factory, String name){
        super(factory, name);
        init();
    }

    public PreparedCrawler(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
        init();
    }

    public void init(){
        buildTaskData();
    }

    //构建请求产品列表的链接及参数和请求头
    private void buildTaskData(){
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
                HttpResponse response=HttpClientUtil.doGet(PRODUCT_LIST_URL,params,headers);
                if(response.getStatusLine().getStatusCode()==200){
                    String responseStr=EntityUtils.toString(response.getEntity());
                    JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
                    JsonObject msg=jsonObject.get("msg").getAsJsonObject();
                    int totalItem=msg.get("total").getAsInt();
                    int totalPage=totalItem%20==0?totalItem/20:totalItem/20+1;
                    for(int j=0;j<totalPage;j++){
                        JsonObject taskData=new JsonObject();
                        params.put("page",j+1+"");
                        taskData.addProperty("url",PRODUCT_LIST_URL);
                        taskData.addProperty("params",gson.toJson(params));
                        taskData.addProperty("headers",gson.toJson(headers));
                        JedisUtil.lpush(FROM_QUEUE,gson.toJson(taskData));
                    }
                }
                else{
                    logger.info("error!failed to request url "+PRODUCT_LIST_URL);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    //获得产品的具体链接
    @SuppressWarnings("unchecked")
    private boolean crawProductUrl(String taskData) throws Exception{
        JsonObject taskDataObject=gson.fromJson(taskData,JsonObject.class);
        String url=taskDataObject.get("url").getAsString();
        Map<String,String> params=gson.fromJson(taskDataObject.get("params").getAsString(),HashMap.class);
        Map<String,String> headers=gson.fromJson(taskDataObject.get("headers").getAsString(),HashMap.class);
        HttpResponse response=HttpClientUtil.doGet(url,params,headers,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),0);
        if(response.getStatusLine().getStatusCode()==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            String html=jsonObject.get("html").getAsString();
            if(html.equals(""))
                return true;
            Document document=Jsoup.parse(html);
            Elements elements=document.getElementsByTag("a");
            for(Element element:elements){
                String href=element.attr("href");
                String productId=href.substring(href.lastIndexOf("/")+1,href.lastIndexOf("."));
                JedisUtil.lpush(TO_QUEUE,productId);
            }
        }
        else{
            logger.info("failed to request "+url+":"+response.getStatusLine().getStatusCode());
            return false;
        }
        return true;
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
            if(!crawProductUrl(taskData)){
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


    public static void main(String[] args) throws Exception{
        /*PreparedCrawler crawler=new PreparedCrawler(null);
        crawler.start();*/
    }
}
