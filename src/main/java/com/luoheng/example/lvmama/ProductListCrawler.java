package com.luoheng.example.lvmama;

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
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductListCrawler extends Crawler{
    static boolean shouldOver=false;
    public static final String FROM_QUEUE="lvmama_task";
    public static final String TO_QUEUE="lvmama_product_href";
    private static final String CITY_LIST_URL="https://login.lvmama.com/seo_api/departureList/getDepartVo.do?channel=zhuzhan&jsoncallback=recive&callback=recive&_=1562315319265";
    //占位符为出发城市编号
    private static final String FIRST_LIST_URL="http://s.lvmama.com/group/H%sK440300?keyword=云南&tabType=route350";
    //占位符为出发城市编号和页数
    private static final String OTHER_LIST_URL="http://s.lvmama.com/group/H%sK440300p%s?keyword=云南&tabType=route350";
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
    //获得出发城市的列表，并以城市编号为参数，构建获取列表任务
    private void buildTask(){
        try{
            HttpResponse response=HttpClientUtil.doGet(CITY_LIST_URL,null,null);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Pattern pattern=Pattern.compile("recive\\((.*)\\)");
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
                            JedisUtil.lpush(FROM_QUEUE,String.format(FIRST_LIST_URL,cityCode));
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
        }catch(IOException e){
            e.printStackTrace();
        }
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

    //获得产品链接
    @Override
    public void crawl(String taskData){
        try{
            HttpResponse response=HttpClientUtil.doGet(taskData,null,null);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                if(document.getElementsByClass("product-item clearfix").size()<=0)
                    return;
                Elements productItems=document.getElementsByClass("product-item clearfix");
                for(Element productItem:productItems){
                    Element a=productItem.getElementsByClass("product-picture").get(0);
                    JedisUtil.lpush(TO_QUEUE,a.attr("href"));
                }
                Elements as=document.getElementsByClass("pagebox").get(0).getElementsByTag("a");
                if(as.size()<=2)
                    return;
                int maxPage=Integer.parseInt(as.get(as.size()-2).ownText().trim());
                List<NameValuePair> forms=new ArrayList<>();
                forms.add(new BasicNameValuePair("ajaxKey","add"));
                UrlEncodedFormEntity entity=new UrlEncodedFormEntity(forms,Charset.forName("UTF-8"));
                for(int i=2;i<=maxPage;i++){
                    response=HttpClientUtil.doPost(taskData.replace("?","P"+i+"?"),
                            null,null,entity);
                    code=response.getStatusLine().getStatusCode();
                    if(code==200){
                        responseStr=EntityUtils.toString(response.getEntity());
                        document=Jsoup.parse(responseStr);
                        if(document.getElementsByClass("product-item clearfix").size()<=0)
                            return;
                        productItems=document.getElementsByClass("product-item clearfix");
                        for(Element productItem:productItems){
                            Element a=productItem.getElementsByClass("product-picture").get(0);
                            JedisUtil.lpush(TO_QUEUE,a.attr("href"));
                        }
                    }
                    else{
                        logger.info("failed to request,code is "+code);
                        JedisUtil.lpush(FROM_QUEUE,taskData);
                        return;
                    }
                }
            }
            else{
                logger.info("failed to request,code is "+code);
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
        }catch(Exception e){
            if(e instanceof IOException){
                JedisUtil.lpush(FROM_QUEUE,getCurrentTask());
                e.printStackTrace();
            }
            else{
                Core.saveErrorMsg(taskData+"\n"+ExceptionUtil.getTotal(e));
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        ProductListCrawler crawler=new ProductListCrawler(null);
        crawler.start();
    }
}
