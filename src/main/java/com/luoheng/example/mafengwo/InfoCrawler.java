package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.CodeUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.*;

public class InfoCrawler extends Crawler{
    private static final String HTML_URL="http://www.mafengwo.cn/sales/%s.html";
    private static final String INDEX_INFO_URL="http://www.mafengwo.cn/sales/detail/index/info";
    private static final String STOCK_INFO_URL="http://www.mafengwo.cn/sales/detail/stock/info";
    public static final String FROM_QUEUE="list_mafengwo_product_id";
    public static final String TO_QUEUE="list_mafengwo_product_info";
    private Logger logger=LogManager.getLogger(InfoCrawler.class);
    private Gson gson;
    public InfoCrawler(CrawlerFactory factory){
        super(factory);
        init();
    }

    public InfoCrawler(CrawlerFactory factory,String name){
        super(factory, name);
        init();
    }

    public InfoCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory, name, crawlInterval);
        init();
    }

    public void init(){
        gson=new Gson();
    }


    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
        //return "2466970";
    }

    /**
     * 获得产品的基本信息和路线
     * @param taskData 产品id
     * @param bean
     * @return
     * @throws IOException
     */
    private boolean crawlIndexInfo(String taskData,Bean bean,List<String> pathList) throws IOException{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("id",taskData);
        headers.put("host","www.mafengwo.cn");
        headers.put("Referer","http://www.mafengwo.cn/sales/6066578.html");
        bean.productId=taskData;
        bean.productLink=String.format(HTML_URL,taskData);
        HttpResponse response=HttpClientUtil.doGet(INDEX_INFO_URL,params,headers,true,number);
        if(response.getStatusLine().getStatusCode()==200){
            HttpEntity entity=response.getEntity();
            String responseStr=EntityUtils.toString(entity);
            JsonObject jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
            JsonObject list=jsonObject.getAsJsonObject("data").getAsJsonObject("list");
            JsonObject ota=list.get("ota").getAsJsonObject();
            //获得旅行社的名字
            bean.taName=ota.get("name").getAsString();
            JsonObject base=list.getAsJsonObject("base");
            //获得产品名字
            bean.productName=base.get("title").getAsString();
            //获得行程路线，因为行程路线所在数组不是固定值，所以通过名字来匹配行程路线。
            //私家游没有路线，所以需要判断并跳过
            JsonArray content=list.getAsJsonArray("content");
            for(int i=0;i<content.size();i++){
                JsonArray nextContent=content.get(i).getAsJsonObject().getAsJsonArray("content");
                int index=-1;
                for(int j=0;j<nextContent.size();j++){
                    if(nextContent.get(j).getAsJsonObject().get("name").getAsString().equals("行程路线")){
                        index=j;
                        break;
                    }
                }
                if(index!=-1){
                    pathList.add(getPath(content.get(i).getAsJsonObject()
                            .getAsJsonArray("content")
                            .get(index).getAsJsonObject().getAsJsonArray("content")));
                }
            }
        }
        else{
            logger.info("failed to request "+taskData+","+response.getStatusLine().getStatusCode());
            return false;
        }
        return true;
    }

    private void saveFailureTask(String taskData){
        logger.info(taskData+" push to queue");
        JedisUtil.lpush(FROM_QUEUE,taskData);
    }


    /**
     *
     * @param bean
     * @return
     * @throws IOException
     */
    private boolean getStockInfo(Bean bean,List<String> pathList) throws IOException{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        headers.put("host","www.mafengwo.cn");
        headers.put("Referer",String.format("http://www.mafengwo.cn/sales/%s.html",bean.productId));
        params.put("groupId",bean.productId);
        HttpResponse response=HttpClientUtil.doGet(STOCK_INFO_URL,params,headers,true,number);
        if(response.getStatusLine().getStatusCode()==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
            JsonObject data=jsonObject.get("data").getAsJsonObject();
            JsonArray sku=data.getAsJsonArray("sku");
            for(int i=0;i<sku.size();i++){
                Bean.Package bPackage=bean.newPackage();
                bean.bPackage=bPackage;
                bPackage.name=sku.get(i).getAsJsonObject().get("name").getAsString();
                bPackage.id=sku.get(i).getAsJsonObject().get("id").getAsString();
                if(pathList.size()==0)
                    bPackage.path="null";
                else if(pathList.size()<i+1)
                    bPackage.path="unusual path";
                else
                    bPackage.path=pathList.get(i);
                if(data.has("depature")){
                    JsonObject departure=data.getAsJsonObject("depature");
                    for(Map.Entry<String,JsonElement> entry:departure.entrySet()){
                        String groupId=entry.getKey();
                        JsonObject value=entry.getValue().getAsJsonObject();
                        bPackage.groupId=groupId;
                        bPackage.cityName=value.get("text").getAsString();
                        JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
                    }
                }
            }
        }
        else{
            logger.info(bean.productId+" failed to request "+STOCK_INFO_URL+" "+response.getStatusLine().getStatusCode());
            return false;
        }
        return true;
    }

    private String getPath(JsonArray content){
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<content.size();i++){
            JsonObject contentItem=content.get(i).getAsJsonObject();
            JsonObject theme=contentItem.getAsJsonObject("theme");
            JsonObject mdd=theme.getAsJsonObject("mdd");
            builder.append("D");
            builder.append(i+1);
            builder.append(":");
            builder.append(mdd.get("label").getAsString());
            if(!theme.has("route")){
                continue;
            }
            JsonArray route=theme.get("route").getAsJsonArray();
            if(route.size()>0){
                JsonObject routeItem=route.get(route.size()-1).getAsJsonObject();
                builder.append("-");
                builder.append(routeItem.getAsJsonObject("position").get("label").getAsString());
            }
        }
        return builder.toString();
    }

    private void saveData(Bean bean){
        JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
    }

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        List<String> pathList=new ArrayList<>();
        try{
            if(!crawlIndexInfo(taskData,bean,pathList)){
                saveFailureTask(taskData);
                return;
            }
        }catch(IOException e){
            if(e instanceof SocketException || e instanceof InterruptedIOException){
                logger.info("request "+taskData+" time out");
                saveFailureTask(taskData);
            }
        }
        try{
            if(!getStockInfo(bean,pathList)){
                saveFailureTask(taskData);
                return;
            }
            saveData(bean);
        }catch(IOException e){
            if(e instanceof SocketException || e instanceof InterruptedIOException){
                saveFailureTask(taskData);
            }
        }
    }
    public static void main(String[] args){
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.start();
    }
}
