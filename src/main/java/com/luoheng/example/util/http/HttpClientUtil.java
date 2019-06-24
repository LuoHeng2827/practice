package com.luoheng.example.util.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HttpClientUtil{
    private static final int DEFAULT_CONNECT_TIMEOUT=5000;
    private static final int DEFAULT_CONNECT_REQUEST_TIMEOUT=5000;
    private static final int DEFAULT_SOCKET_TIMEOUT=5000;
    private static final int DEFAULT_PROXY_CONNECT_TIMEOUT=15000;
    private static final int DEFAULT_PROXY_CONNECT_REQUEST_TIMEOUT=15000;
    private static final int DEFAULT_PROXY_SOCKET_TIMEOUT=15000;
    private Logger logger=LogManager.getLogger(HttpClientUtil.class);

    public static HttpResponse doGet(String url,Map<String,String> params,Map<String,String> headers)
            throws IOException{
        return doGet(url, params, headers,false,-1);
    }


    public static HttpResponse doGet(String url,Map<String,String> params,Map<String,String> headers,
                                     boolean isProxy,int number) throws IOException{
        if(headers==null)
            headers=new HashMap<>();
        if(params==null)
            params=new HashMap<>();
        CloseableHttpClient client=HttpClientBuilder.create().build();
        HttpGet httpGet=new HttpGet(generateGetParams(url, params));
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

    private static HttpHost getHttpHost(int number){
        Gson gson=new Gson();
        Jedis jedis=JedisUtil.getResource();
        String jsonStr=jedis.get("proxy");
        JsonArray jsonArray=gson.fromJson(jsonStr,JsonArray.class);
        int index=number%jsonArray.size();
        JsonObject jsonObject=jsonArray.get(index).getAsJsonObject();
        return new HttpHost(jsonObject.get("host").getAsString(),
                Integer.parseInt(jsonObject.get("port").getAsString()));
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
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        params.put("id","6066578");
        for(int i=0;i<50;i++){
            HttpResponse response=doGet("http://www.mafengwo.cn/sales/detail/stock/detail",
                    params,null,true,1);
            if(response.getStatusLine().getStatusCode()==200){
                System.out.println("request ok");
            }
            else{
                System.out.println("error "+response.getStatusLine().getStatusCode());
            }
        }
    }
}
