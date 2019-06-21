package com.luoheng.example.mafengwo;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.CodeUtil;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailCrawler extends Crawler{
    private static final String HTML_URL="http://www.mafengwo.cn/sales/%s.html";
    private static final String INDEX_URL="http://www.mafengwo.cn/sales/detail/index/info";
    private static final String CITY_URL="http://www.mafengwo.cn/sales/detail/stock/info";
    private static final String PRICE_URL="http://www.mafengwo.cn/sales/detail/stock/detail";
    public static final String FROM_QUEUE="list_mafengwo_product_id";
    public static final String TO_QUEUE="list_mafengwo_tour_db";
    private Logger logger=LogManager.getLogger(DetailCrawler.class);
    private Gson gson;
    private Proxy proxy;
    public DetailCrawler(CrawlerFactory factory){
        super(factory);
    }

    public DetailCrawler(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public DetailCrawler(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    public DetailCrawler proxy(Proxy proxy){
        this.proxy=proxy;
        return this;
    }

    @Override
    public void init(){
        super.init();
        gson=new Gson();
    }

    @Override
    public String getTaskData(){
        Jedis jedis=JedisUtil.getResource();
        String productUrl=jedis.lpop(FROM_QUEUE);
        jedis.close();
        return productUrl;
        //return "2466970";
    }

    private boolean getDetailIndex(String taskData, Bean bean){
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("id",taskData);
        bean.productId=taskData;
        bean.productLink=String.format(HTML_URL,taskData);
        try{
            Response response=HttpUtil.doGet(INDEX_URL,params,headers,proxy);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
                JsonObject list=jsonObject.getAsJsonObject("data").getAsJsonObject("list");
                JsonObject ota=list.get("ota").getAsJsonObject();
                bean.taName=ota.get("name").getAsString();
                JsonObject base=list.getAsJsonObject("base");
                bean.productName=base.get("title").getAsString();
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
                    if(index!=-1)
                        getPath(content.get(i).getAsJsonObject()
                                .getAsJsonArray("content")
                                .get(index).getAsJsonObject().getAsJsonArray("content"),bean);
                }
            }
            else{
                logger.info("failed to request "+response.request().url());
                return false;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return true;
    }

    private boolean isPackageExist(List<Bean.Package> packageList,String packageName){
        if(getPackageByName(packageList, packageName)!=null)
            return true;
        return false;
    }

    private Bean.Package getPackageByName(List<Bean.Package> packageList,String packageName){
        for(Bean.Package p:packageList){
            if(p.name.equals(packageName))
                return p;
        }
        return null;
    }


    private boolean getPackageList(Bean bean){
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("groupId",bean.productId);
        try{
            Response response=HttpUtil.doGet(CITY_URL,params,headers,proxy);
            if(response.code()==200){
                String responseStr=response.body().string();
                JsonObject jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
                JsonObject data=jsonObject.get("data").getAsJsonObject();
                JsonArray sku=data.getAsJsonArray("sku");
                String[] skuArray=new String[sku.size()];
                for(int i=0;i<sku.size();i++){
                    skuArray[i]=sku.get(i).getAsJsonObject().get("id").getAsString();
                }
                if(!data.has("depature")){
                    String url=PRICE_URL+"?"+"groupId="+bean.productId;
                    for(String skuId:skuArray){
                        url+="&"+"skuIds[]="+skuId;
                    }
                    response=HttpUtil.doGet(url,params,headers,proxy);
                    if(response.code()==200){
                        responseStr=response.body().string();
                        jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
                        sku=jsonObject.getAsJsonObject("data")
                                .getAsJsonArray("sku");
                        for(int i=0;i<sku.size();i++){
                            JsonObject skuItem=sku.get(i).getAsJsonObject();
                            JsonArray calendar=skuItem.get("calendar").getAsJsonArray();
                            String packageName=skuItem.get("name").getAsString();
                            Bean.Package bPackage=null;
                            if(isPackageExist(bean.packageList,packageName)){
                                bPackage=getPackageByName(bean.packageList,packageName);
                            }
                            else{
                                bPackage=bean.newPackage();
                                bean.packageList.add(bPackage);
                            }
                            bPackage.name=packageName;
                            Bean.Calendar bCalendar=bean.newCalendar();
                            bPackage.calendarList.add(bCalendar);
                            bCalendar.cityName="无";
                            int cSize=Math.min(7,calendar.size());
                            for(int j=0;j<cSize;j++){
                                JsonObject calendarItem=calendar.get(j).getAsJsonObject();
                                bCalendar.map.put(calendarItem.get("date").getAsString(),
                                        calendarItem.get("price").getAsString());
                            }
                        }

                    }
                    else{
                        logger.info("failed to request url "+response.body().string());
                        return false;
                    }
                }
                else{
                    JsonObject departure=data.getAsJsonObject("depature");
                    for(Map.Entry<String, JsonElement> entry:departure.entrySet()){
                        String key=entry.getKey();
                        JsonObject value=entry.getValue().getAsJsonObject();
                        String cityName=value.get("text").getAsString();
                        String url=PRICE_URL+"?"+"groupId="+key;
                        for(String skuId:skuArray){
                            url=url+"&"+"skuIds[]="+skuId;
                        }
                        response=HttpUtil.doGet(url,params,headers,proxy);
                        if(response.code()==200){
                            responseStr=response.body().string();
                            jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
                            sku=jsonObject.getAsJsonObject("data")
                                    .getAsJsonArray("sku");
                            for(int i=0;i<sku.size();i++){
                                JsonObject skuItem=sku.get(i).getAsJsonObject();
                                JsonArray calendar=skuItem.get("calendar").getAsJsonArray();
                                String packageName=skuItem.get("name").getAsString();
                                Bean.Package bPackage=null;
                                if(isPackageExist(bean.packageList,packageName)){
                                    bPackage=getPackageByName(bean.packageList,packageName);
                                }
                                else{
                                    bPackage=bean.newPackage();
                                    bean.packageList.add(bPackage);
                                }
                                bPackage.name=packageName;
                                Bean.Calendar bCalendar=bean.newCalendar();
                                bPackage.calendarList.add(bCalendar);
                                bCalendar.cityName=cityName;
                                for(int j=0;j<calendar.size();j++){
                                    JsonObject calendarItem=calendar.get(j).getAsJsonObject();
                                    String date=calendarItem.get("date").getAsString();
                                    String price=calendarItem.get("price").getAsString();
                                    if(!bCalendar.map.containsKey(date)){
                                        bCalendar.map.put(date,price);
                                    }
                                }
                            }

                        }
                        else{
                            logger.info("failed to request url "+response.body().string());
                            return false;
                        }
                    }
                }

            }
            else{
                logger.info("failed to request "+response.request().url());
                return false;
        }
        }catch(IOException e){
        e.printStackTrace();
        }
        return true;
    }

    private void getPath(JsonArray content,Bean bean){
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
        bean.pathList.add(builder.toString());
    }

    private void saveData(Bean bean){
        Jedis jedis=JedisUtil.getResource();
        String json=gson.toJson(bean);
        jedis.lpush(TO_QUEUE,json);
        jedis.close();
    }

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        if(!getDetailIndex(taskData,bean)||!getPackageList(bean)){
            Jedis jedis=JedisUtil.getResource();
            jedis.lpush(FROM_QUEUE,bean.productId);
            jedis.close();
            return;
        }
        saveData(bean);
    }
    public static void main(String[] args){
        DetailCrawler crawler=new DetailCrawler(null);
        crawler.start();
    }
}
