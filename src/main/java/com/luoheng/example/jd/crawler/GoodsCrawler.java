package com.luoheng.example.jd.crawler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.LogUtil;
import com.luoheng.example.jd.bean.Goods;
import com.luoheng.example.jd.service.GoodsService;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component

public class GoodsCrawler implements Runnable{
    private static final String TARGET_URL="https://so.m.jd.com/ware/search._m2wq_list";
    private static final String imagePrefix="http://img14.360buyimg.com/mobilecms/s300x300_";
    private static final byte[] redisKey_db="list_jd_sku".getBytes();
    Gson gson=new Gson();
    private JedisUtil jedisUtil;
    private GoodsService goodsService;

    Pattern pattern=Pattern.compile("searchCB\\(([\\d\\D]*)\\)");
    private String keyWord;

    public GoodsCrawler keyword(String keyWord){
        this.keyWord=keyWord;
        return this;
    }

    public void setKeyWord(String keyWord) {
        this.keyWord = keyWord;
    }

    private String getTargetUrl(){
        return "https://mall.jd.com/view_search-427364-0-99-1-24-1.html";
    }

    private String correctJsonResult(String json){
        /*Matcher matcher=pattern.matcher(json);
        if(matcher.matches()){
            json=matcher.group(1);
        }*/
        json=json.replace("searchCB(","");
        json=json.substring(0,json.length()-2);
        return json.trim();
    }

    private List<Goods> parseData(String json){
        List<Goods> goodsList=new ArrayList<>();
        JsonObject result= gson.fromJson(json,JsonObject.class);
        JsonArray paragraph=result.getAsJsonObject("data")
                .getAsJsonObject("searchm")
                .getAsJsonArray("Paragraph");
        for(int i=0;i<paragraph.size();i++){
            Goods goods=new Goods();
            JsonObject ji=paragraph.get(i).getAsJsonObject();
            JsonObject content=ji.getAsJsonObject("Content");
            goods.setImageUrl(imagePrefix+content.get("imageurl").getAsString());
            goods.setName(content.get("warename").getAsString());
            goods.setWareId(ji.get("wareid").getAsString());
            goods.setPrice(ji.get("dredisprice").getAsString());
            goodsList.add(goods);
        }
        return goodsList;
    }

    private int getTargetCount(String keyword){
        String json=requestData(keyword,-1,-1);
        JsonObject object=gson.fromJson(json,JsonObject.class);
        JsonObject summary=object.getAsJsonObject("data")
                .getAsJsonObject("searchm")
                .getAsJsonObject("Head")
                .getAsJsonObject("Summary");
        return summary.get("OrgSkuCount").getAsInt();
    }

    private String requestData(String keyword,int page,int pageSize){
        Map<String,String> params=new HashMap<>();
        params.put("keyword",keyword);
        if(page!=-1)
            params.put("page",page+"");
        if(page!=-1)
            params.put("pagesize",pageSize+"");
        try{
            Response response=HttpUtil.doGet(TARGET_URL,params,null);
            if(response.code()==200)
                return correctJsonResult(response.body().string());
            else{
                //todo
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
    public void test(){
        //String json=requestData("联合利华京东自营旗舰店",1,5);
        String json="st)";
        LogUtil.i(json.substring(0,json.length()-2));
        /*Matcher matcher=pattern.matcher(json);
        if(matcher.matches()) {
            json=matcher.group(1);
            LogUtil.i("true");
        }*/
    }

    @Override
    public void run() {
        Jedis jedis=jedisUtil.getResource();
        int count=getTargetCount(keyWord);
        String json=requestData(keyWord,-1,count);
        List<Goods> goodsList=parseData(json);
        for(Goods goods:goodsList){
            //LogUtil.i(goods.toString());
            try{
                jedis.lpush(redisKey_db,goods.getWareId().getBytes("UTF-8"));
                goodsService.saveGoods(goods);
            }catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
        jedisUtil.returnResource(jedis);
    }

    @Autowired
    public void setJedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    @Autowired
    public void setGoodsService(GoodsService goodsService) {
        this.goodsService = goodsService;
    }


    public static void main(String[] args) throws Exception{
        GoodsCrawler goodsCrawler =new GoodsCrawler();
        new Thread(goodsCrawler).start();
    }
}
