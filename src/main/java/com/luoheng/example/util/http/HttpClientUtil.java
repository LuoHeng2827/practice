package com.luoheng.example.util.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpClientUtil{
    private static final int DEFAULT_CONNECT_TIMEOUT=5000;
    private static final int DEFAULT_CONNECT_REQUEST_TIMEOUT=5000;
    private static final int DEFAULT_SOCKET_TIMEOUT=5000;
    private static final int DEFAULT_PROXY_CONNECT_TIMEOUT=15000;
    private static final int DEFAULT_PROXY_CONNECT_REQUEST_TIMEOUT=15000;
    private static final int DEFAULT_PROXY_SOCKET_TIMEOUT=15000;
    private static Logger logger=LogManager.getLogger(HttpClientUtil.class);

    public static HttpResponse doGet(String url,Map<String,String> params,Map<String,String> headers)
            throws IOException{
        return doGet(url, params, headers,false,-1);
    }

    private static void buildDefaultHeader(HttpRequestBase base){
        base.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
        base.setHeader("Accept","*/*");
        base.setHeader("Cache-Control","no-cache");
        base.setHeader("Connection","keep-alive");
    }

    private static void addHeaders(HttpRequestBase base,Map<String,String> headers){
        buildDefaultHeader(base);
        for(Map.Entry<String,String> entry:headers.entrySet()){
            base.setHeader(entry.getKey(),entry.getValue());
        }
    }

    public static HttpResponse doGet(String url,Map<String,String> params,Map<String,String> headers,
                                     boolean isProxy,int number) throws IOException{
        if(headers==null)
            headers=new HashMap<>();
        if(params==null)
            params=new HashMap<>();
        CloseableHttpClient client=HttpClientBuilder.create().build();
        HttpGet httpGet=new HttpGet(generateGetParams(url, params));
        addHeaders(httpGet,headers);
        RequestConfig.Builder configBuilder=null;
        if(isProxy){
            configBuilder=RequestConfig.custom()
                    .setConnectTimeout(DEFAULT_PROXY_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_PROXY_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_PROXY_SOCKET_TIMEOUT)
                    .setProxy(getHttpHost(number));
        }
        else{
            configBuilder=RequestConfig.custom()
                    .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        }
        RequestConfig config=configBuilder.build();
        httpGet.setConfig(config);
        for(Map.Entry<String,String> entry:headers.entrySet()){
            httpGet.addHeader(entry.getKey(),entry.getValue());
        }
        return client.execute(httpGet);
    }

    public static HttpResponse doPost(String url,Map<String,String> params,Map<String,String> headers,
                                      HttpEntity entity) throws IOException{
        if(headers==null)
            headers=new HashMap<>();
        if(params==null)
            params=new HashMap<>();
        CloseableHttpClient client=HttpClientBuilder.create().build();
        HttpPost httpPost=new HttpPost(generateGetParams(url, params));
        httpPost.setEntity(entity);
        return client.execute(httpPost);
    }

    private static HttpHost getHttpHost(int number){
        Gson gson=new Gson();
        String jsonStr=JedisUtil.get("proxy");
        while(jsonStr==null){
            ThreadUtil.waitSecond(1);
            jsonStr=JedisUtil.get("proxy");
        }
        JsonArray jsonArray=gson.fromJson(jsonStr,JsonArray.class);
        number=number<0?0:number;
        int index=number%jsonArray.size();
        JsonObject jsonObject=jsonArray.get(index).getAsJsonObject();
        String host=jsonObject.get("host").getAsString();
        int port=Integer.parseInt(jsonObject.get("port").getAsString());
        //logger.info(host+":"+port   );
        return new HttpHost(host,port);
    }

    private static String generateGetParams(String url,Map<String,String> params){
        if(params.size()==0)
            return url;
        StringBuilder stringBuilder=new StringBuilder(url+"?");
        for(Map.Entry<String,String> entry:params.entrySet()){
            stringBuilder.append(entry.getKey()+"="+entry.getValue()+"&");
        }
        stringBuilder.delete(stringBuilder.lastIndexOf("&"),stringBuilder.length());
        return stringBuilder.toString();
    }


    public static void main(String[] args) throws Exception{

    }
}
