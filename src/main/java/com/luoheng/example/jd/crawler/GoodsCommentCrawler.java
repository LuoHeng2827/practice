package com.luoheng.example.jd.crawler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.LogUtil;
import com.luoheng.example.jd.bean.GoodsComment;
import com.luoheng.example.jd.service.GoodsCommentService;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoodsCommentCrawler implements Runnable{
    private static final String TARGET_URL="https://wq.jd.com/commodity/comment/getcommentlist";
    private static final byte[] redisKey_task="list_jd_sku".getBytes();
    private static final byte[] redisKey_db="list_jd_comment".getBytes();
    private JedisUtil jedisUtil;
    private Gson gson=new Gson();
    private GoodsCommentService goodsCommentService;

    private String getTargetUrl(){
        return TARGET_URL;
    }
    private String correctJsonResult(String json){
        json=json.replace("commentCB(","").replace("客户端\",","客户端\"");
        json=json.substring(0,json.length()-1);
        json=json.replaceAll("\\s","").replaceAll("\\\\x","\\\\\\\\x").replaceAll(",\\}","\\}");
        return json;
    }

    private List<GoodsComment> parseData(String json, int j){
        List<GoodsComment> goodsCommentList =new ArrayList<>();
        JsonObject object=null;
        try{
            object=gson.fromJson(json,JsonObject.class);
        }catch(JsonSyntaxException e){
            LogUtil.i(j+"");
            e.printStackTrace();
        }
        if(object==null){
            LogUtil.i(json);
            return goodsCommentList;
        }
        JsonObject result=object.getAsJsonObject("result");
        JsonArray comments=result.getAsJsonArray("comments");
        for(int i=0;i<comments.size();i++){
            JsonObject ji=comments.get(i).getAsJsonObject();
            GoodsComment goodsComment =new GoodsComment();
            goodsComment.setContent(ji.get("content").getAsString());
            goodsCommentList.add(goodsComment);
        }
        return goodsCommentList;
    }

    private int getTargetCount(String sku){
        String json=requestData(sku,-1,-1);
        JsonObject object=null;
        try{
            object=gson.fromJson(json,JsonObject.class);
        }catch(JsonSyntaxException e){
            LogUtil.i("test ");
        }
        JsonObject productCommentSummary=object.getAsJsonObject("result").getAsJsonObject("productCommentSummary");
        return productCommentSummary.get("CommentCount").getAsInt();
    }

    private String requestData(String sku,int page,int pageSize){
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("sku",sku);
        params.put("sorttype","5");
        if(page!=-1)
            params.put("page",page+"");
        if(pageSize!=-1)
            params.put("pagesize",pageSize+"");
        headers.put("Referer","https://item.m.jd.com/product/8699365.html");
        headers.put("Accept","*/*");
        headers.put("Host","wq.jd.com");
        headers.put("Accept-Charset","utf-8");
        headers.put("accept-encoding","gzip, deflate");
        headers.put("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Mobile Safari/537.36");
        try{
            Response response=HttpUtil.doGet(getTargetUrl(),params,headers);
            if(response.code()==200)
                return correctJsonResult(response.body().string());
            else{
                LogUtil.i("error "+response.toString());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        Jedis jedis=jedisUtil.getResource();
        byte[] msg=jedis.lpop(redisKey_task);
        if(msg==null){
            return;
        }
        String sku=new String(msg);
        int count=getTargetCount(sku);
        int totalPage=count%10==0?count/10:count/10+1;
        List<GoodsComment> goodsCommentList =new ArrayList<>(count);
        for(int i=1;i<=totalPage;i++){
            String json=requestData(sku,i,10);
            List<GoodsComment> cl=parseData(json,i);
            goodsCommentList.addAll(cl);
        }
        for(GoodsComment goodsComment : goodsCommentList){
            LogUtil.i(goodsComment.toString());
            goodsCommentService.saveGoodsComment(goodsComment);
        }
        jedisUtil.returnResource(jedis);
    }

    @Autowired
    public void setGoodsCommentService(GoodsCommentService goodsCommentService) {
        this.goodsCommentService = goodsCommentService;
    }

    @Autowired
    public void setJedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    public static void main(String[] args){
        GoodsCommentCrawler goodsCommentCrawler =new GoodsCommentCrawler();
    }
}
