package com.luoheng.example.mafengwo;

import com.google.gson.*;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.CodeUtil;
import com.luoheng.example.util.ExceptionUtil;
import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpEntity;
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
 * 爬取产品信息
 */
public class InfoCrawler extends Crawler{
    private static final String HTML_URL="http://www.mafengwo.cn/sales/%s.html";
    //获得产品基本信息的链接
    private static final String INDEX_INFO_URL="http://www.mafengwo.cn/sales/detail/index/info";
    //获得套餐的相关信息及路线
    private static final String STOCK_INFO_URL="http://www.mafengwo.cn/sales/detail/stock/info";
    //获得价格日历的相关信息
    private static final String DETAIL_URL="http://www.mafengwo.cn/sales/detail/stock/detail";
    public static final String FROM_QUEUE="list_mafengwo_product_task";
    public static final String TO_QUEUE="list_mafengwo_db";
    private Logger logger=LogManager.getLogger(InfoCrawler.class);
    private Gson gson;
    private HttpResponse response;

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
    }

    /**
     * 获得产品的基本信息和路线
     * @param taskData 产品id
     * @param bean
     * @return
     * @throws Exception
     */
    private boolean crawlIndexInfo(String taskData,Bean bean,List<String> pathList) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("id",taskData);
        headers.put("host","www.mafengwo.cn");
        headers.put("Referer","http://www.mafengwo.cn/sales/6066578.html");
        bean.productId=taskData;
        bean.productLink=String.format(HTML_URL,taskData);
        this.response=HttpClientUtil.doGet(INDEX_INFO_URL,params,headers,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        HttpResponse response=this.response;
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
                    pathList.add(crawlPath(content.get(i).getAsJsonObject()
                            .getAsJsonArray("content")
                            .get(index).getAsJsonObject().getAsJsonArray("content")));
                }
            }
        }
        else{
            logger.info("failed to request "+taskData+",code is "+response.getStatusLine().getStatusCode());
            return false;
        }
        return true;
    }

    /**
     * 获取产品价格日历
     * @param bean
     * @param packageIds
     * @return
     * @throws Exception
     */
    private boolean crawlCalendar(Bean bean,String[] packageIds) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> header=new HashMap<>();
        params.put("groupId",bean.productId);
        for(String packageId:packageIds){
            params.put("skuIds[]",packageId);
        }
        HttpResponse response=HttpClientUtil.doGet(DETAIL_URL,params,header,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int responseCode=response.getStatusLine().getStatusCode();
        if(responseCode==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            if(jsonObject.get("data") instanceof JsonNull)
                return true;
            JsonArray sku=jsonObject.getAsJsonObject("data")
                    .getAsJsonArray("sku");
            for(int i=0;i<sku.size();i++){
                JsonObject skuItem=sku.get(i).getAsJsonObject();
                JsonArray calendar=skuItem.get("calendar").getAsJsonArray();
                int saveDay=Math.min(7,calendar.size()/2);
                Map<String,String> calendarMap=new HashMap<>();
                bean.priceList.clear();
                for(int j=0;j<calendar.size()&&calendarMap.size()<saveDay;j++){
                    JsonObject calendarItem=calendar.get(j).getAsJsonObject();
                    String dateStr=calendarItem.get("date").getAsString();
                    String priceStr=calendarItem.get("price").getAsString();
                    if(calendarMap.containsKey(dateStr))
                        continue;
                    calendarMap.put(dateStr,priceStr);
                    Bean.Price price=bean.newPrice();
                    price.price=Float.parseFloat(priceStr);
                    price.date=dateStr;
                    bean.priceList.add(price);
                }
                //logger.info(gson.toJson(bean));
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
            }
        }
        else{
            logger.info("failed to request,code is "+responseCode);
            return false;
        }
        return true;
    }

    /**
     * 获得套餐的数量和信息，并将保存的路线传给相应的套餐
     * @param bean
     * @param pathList
     * @return
     * @throws Exception
     */
    private boolean getStockInfo(Bean bean,List<String> pathList) throws Exception{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        headers.put("host","www.mafengwo.cn");
        headers.put("Referer",String.format("http://www.mafengwo.cn/sales/%s.html",bean.productId));
        params.put("groupId",bean.productId);
        HttpResponse response=HttpClientUtil.doGet(STOCK_INFO_URL,params,headers,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        if(response.getStatusLine().getStatusCode()==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(CodeUtil.unicodeToChinese(responseStr),JsonObject.class);
            JsonObject data=jsonObject.get("data").getAsJsonObject();
            JsonArray sku=data.getAsJsonArray("sku");
            String[] packageIds=new String[sku.size()];
            //遍历套餐
            for(int i=0;i<sku.size();i++){
                Bean.Package bPackage=bean.newPackage();
                bean.bPackage=bPackage;
                bPackage.name=sku.get(i).getAsJsonObject().get("name").getAsString();
                packageIds[i]=sku.get(i).getAsJsonObject().get("id").getAsString();
                if(pathList.size()==0)
                    bPackage.path="null";
                else if(pathList.size()<i+1)
                    bPackage.path="unusual path";
                else
                    bPackage.path=pathList.get(i);
            }
            return crawlCalendar(bean,packageIds);
        }
        else{
            logger.info(bean.productId+" failed to request "+STOCK_INFO_URL+" "+response.getStatusLine().getStatusCode());
            return false;
        }
    }

    /**
     * 爬取产品路线
     * @param content
     * @return
     */
    private String crawlPath(JsonArray content){
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


    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        List<String> pathList=new ArrayList<>();
        try{
            if(!crawlIndexInfo(taskData,bean,pathList)||!getStockInfo(bean,pathList)){
                JedisUtil.lpush(FROM_QUEUE,taskData);
                return;
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
        ThreadUtil.waitSecond(2);
    }
    public static void main(String[] args){
        InfoCrawler crawler=new InfoCrawler(null);
        crawler.crawl("2466970");
    }
}
