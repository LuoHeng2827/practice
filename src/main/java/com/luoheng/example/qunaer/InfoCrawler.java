package com.luoheng.example.qunaer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//负责采集产品的信息
public class InfoCrawler extends Crawler{
    public static final String FROM_QUEUE="qunaer_product_href";
    public static final String TO_QUEUE="qunaer_product_db";
    //获得路线的链接
    private static final String PATH_URL="https://yntq1.package.qunar.com/user/detail/getSchedule.json";
    //获得日历的链接，占位符表示HOST(有多种HOST)
    private static final String CALENDAR_URL="https://%s/api/calPrices.json";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(InfoCrawler.class);
    public InfoCrawler(CrawlerFactory factory){
        super(factory);
    }

    public InfoCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public InfoCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }

    //从产品链接获得产品id
    private String getProductIdByUrl(String productUrl){
        Pattern pattern=Pattern.compile(".*p*id=(\\d+)&*.*");
        Matcher matcher=pattern.matcher(productUrl);
        if(matcher.matches())
            return matcher.group(1);
        return "";
    }

    //获得日历的价钱和时间
    private boolean crawlCalendar(String productUrl,Bean bean) throws Exception{
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM");
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("tuId","");
        params.put("pId",getProductIdByUrl(productUrl));
        params.put("month",format.format(new Date()));
        params.put("caller","detail");
        params.put("_",new Date().getTime()+"");
        headers.put("cookies","QN48=tc_2ce8863580e7e9c9_16b1c9cb6ca_e124; QN300=ctrip.com; QN1=eIQjm1z05ImAzhUDCM1pAg==");
        headers.put("Referer",productUrl);
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        headers.put("X-Requested-With","XMLHttpRequest");
        URL url=new URL(productUrl);
        HttpResponse response=HttpClientUtil.doGet(String.format(CALENDAR_URL,url.getHost()),params,headers,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            if(jsonObject.getAsJsonObject("data").get("team") instanceof JsonNull)
                return true;
            JsonArray team=jsonObject.getAsJsonObject("data").getAsJsonArray("team");
            int day=Math.min(7,team.size());
            for(int i=0;i<day;i++){
                JsonObject object=team.get(i).getAsJsonObject();
                String date=object.get("date").getAsString();
                float price;
                if(object.getAsJsonObject("prices").get("taocan_price") instanceof JsonNull)
                    price=object.getAsJsonObject("prices").get("adultPrice").getAsFloat();
                else
                    price=object.getAsJsonObject("prices").get("taocan_price").getAsFloat();
                Bean.Price bPrice=bean.newPrice(date,price);
                bean.priceList.add(bPrice);
            }
        }
        else{
            logger.info("failed to request "+CALENDAR_URL+",code is "+code);
            JedisUtil.lpush(FROM_QUEUE,productUrl);
            return false;
        }
        return true;
    }

    //爬取路线
    private boolean crawlPath(String productUrl,Bean bean) throws Exception{
        Map<String,String> params=new HashMap<>();
        params.put("pId",getProductIdByUrl(productUrl));
        params.put("isVer","false");
        params.put("oid","");
        HttpResponse response=HttpClientUtil.doGet(PATH_URL,params,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            JsonObject jsonObject=gson.fromJson(responseStr,JsonObject.class);
            JsonArray dailySchedules=jsonObject.getAsJsonObject("data")
                    .getAsJsonArray("dailySchedules");
            StringBuilder builder=new StringBuilder();
            for(int i=0;i<dailySchedules.size();i++){
                JsonObject scheduleItem=dailySchedules.get(i).getAsJsonObject();
                builder.append("D");
                builder.append(i+1);
                builder.append(":");
                builder.append(scheduleItem.get("dayTitle").getAsString());
            }
            bean.bPackage.path=builder.toString();
        }
        else{
            logger.info("failed to request "+PATH_URL+",code is "+code);
            JedisUtil.lpush(FROM_QUEUE,productUrl);
            return false;
        }
        return true;
    }

    //获取基本信息
    private boolean crawlInfo(String productUrl,Bean bean) throws Exception{
        bean.bPackage.name="跟团游";
        Map<String,String> headers=new HashMap<>();
        headers.put("cookies","QN48=tc_2ce8863580e7e9c9_16b1c9cb6ca_e124; QN300=ctrip.com; QN1=eIQjm1z05ImAzhUDCM1pAg==");
        HttpResponse response=HttpClientUtil.doGet(productUrl,null,null,
                Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
        int code=response.getStatusLine().getStatusCode();
        if(code==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            Document document=Jsoup.parse(responseStr);
            Pattern pattern=Pattern.compile("[\\s\\S]*\\'(.*)\\'[\\s\\S]*");
            Matcher matcher=pattern.matcher(document.getElementsByTag("script").get(0).data());
            if(matcher.matches()){
                String url="https:"+matcher.group(1);
                if(!productUrl.contains("dujia.qunar.com"))
                    url=productUrl;
                response=HttpClientUtil.doGet(url,null,headers,
                        Boolean.valueOf(PropertiesUtil.getValue("proxy.use")),number);
                code=response.getStatusLine().getStatusCode();
                if(code==200){
                    responseStr=EntityUtils.toString(response.getEntity());
                    document=Jsoup.parse(responseStr);
                    bean.productName=document.getElementsByTag("h1").get(0).text().trim();
                    Element summary=document.getElementsByClass("summary").get(0);
                    Element order=summary.getElementsByClass("order").get(1);
                    bean.productId=order.getElementsByTag("li").get(0).getElementsByTag("span").get(0).ownText();
                    Element basicInfo=order.getElementsByClass("basic-info").first();
                    bean.bPackage.cityName=basicInfo.getElementsByTag("em").get(0)
                            .ownText().replace("出发","").replaceAll("\\(.*\\)","");
                    bean.duration=basicInfo.getElementsByTag("em").get(1).ownText();
                    if(document.getElementsByClass("pack js-taocan-item active").size()>0){
                        bean.bPackage.name=document.getElementsByClass("pack js-taocan-item active")
                                .first().ownText();
                    }
                    bean.taName=document.getElementsByClass("common-list business-info").first()
                            .getElementsByTag("li").first().getElementsByTag("em").first().ownText();
                    return crawlPath(url,bean) && crawlCalendar(url,bean);
                }
                else{
                    logger.info("failed to request "+productUrl+" ,code is "+code);
                    JedisUtil.lpush(FROM_QUEUE,productUrl);
                    return false;
                }
            }

        }
        else{
            logger.info("failed to request "+productUrl+" ,code is "+code);
            JedisUtil.lpush(FROM_QUEUE,productUrl);
            return false;
        }
        return true;
    }

    @Override
    public void crawl(String taskData){
        Bean bean=new Bean();
        bean.productLink=taskData;
        try{
            if(!crawlInfo(taskData,bean)){
                JedisUtil.lpush(FROM_QUEUE,taskData);
            }
            else{
                //logger.info(gson.toJson(bean));
                JedisUtil.lpush(TO_QUEUE,gson.toJson(bean));
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
        ThreadUtil.waitSecond(1);
    }

    public static void main(String[] args){
        InfoCrawler crawler=new InfoCrawler(null);
        String s="阿克苏地区出发(含阿克苏地区-乌鲁木齐的交通)";
        System.out.println(s.replaceAll("\\(.*\\)",""));
        //crawler.crawl("https://dujia.qunar.com/pi/detail_33942385?vendor=%E9%A9%B4%E5%A6%88%E5%A6%88%E6%97%85%E6%B8%B8&function=%E8%B7%9F%E5%9B%A2%E6%B8%B8&departure=%E4%B8%B9%E4%B8%9C&arrive=%E4%B8%BD%E6%B1%9F&ttsRouteType=%E6%9C%AC%E5%9C%B0%E6%B8%B8&filterDate=2019-07-17,2019-07-17");
        /*List<String> list=JedisUtil.lrange(FROM_QUEUE,0,JedisUtil.llen(FROM_QUEUE)-1);
        for(String url:list){
            if(url.contains("tuId="))
                System.out.println(url);
        }*/
    }
}
