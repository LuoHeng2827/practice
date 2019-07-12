package com.luoheng.example.util.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpClientUtil{
    private static final int DEFAULT_CONNECT_TIMEOUT=10000;
    private static final int DEFAULT_CONNECT_REQUEST_TIMEOUT=10000;
    private static final int DEFAULT_SOCKET_TIMEOUT=10000;
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

    public static List<Cookie> getDoGetCookie(String url,Map<String,String> params,Map<String,String> headers)
            throws IOException{
        if(headers==null)
            headers=new HashMap<>();
        if(params==null)
            params=new HashMap<>();
        CookieStore cookieStore=new BasicCookieStore();
        CloseableHttpClient client=HttpClients.custom().setDefaultCookieStore(cookieStore).build();
        HttpGet httpGet=new HttpGet(generateGetParams(url,params));
        addHeaders(httpGet,headers);
        RequestConfig.Builder configBuilder=RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT)
                .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        httpGet.setConfig(configBuilder.build());
        client.execute(httpGet);
        return cookieStore.getCookies();
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
        RequestConfig.Builder configBuilder;
        if(isProxy){
            configBuilder=RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(DEFAULT_PROXY_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_PROXY_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_PROXY_SOCKET_TIMEOUT)
                    .setProxy(getHttpHost(number));
        }
        else{
            configBuilder=RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        }
        RequestConfig config=configBuilder.build();
        httpGet.setConfig(config);
        return client.execute(httpGet);
    }

    public static HttpResponse doPost(String url,Map<String,String> params,Map<String,String> headers,
                                      HttpEntity entity) throws IOException{
        return doPost(url, params, headers, entity,false,-1);
    }

    public static HttpResponse doPost(String url,Map<String,String> params,Map<String,String> headers,
                                      HttpEntity entity,boolean isProxy,int number) throws IOException{
        if(headers==null)
            headers=new HashMap<>();
        if(params==null)
            params=new HashMap<>();
        if(entity==null)
            entity=new StringEntity("");
        CloseableHttpClient client=HttpClientBuilder.create().build();
        HttpPost httpPost=new HttpPost(generateGetParams(url, params));
        addHeaders(httpPost,headers);
        httpPost.setEntity(entity);
        RequestConfig.Builder configBuilder;
        if(isProxy){
            configBuilder=RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(DEFAULT_PROXY_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_PROXY_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_PROXY_SOCKET_TIMEOUT)
                    .setProxy(getHttpHost(number));
        }
        else{
            configBuilder=RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .setConnectionRequestTimeout(DEFAULT_CONNECT_REQUEST_TIMEOUT)
                    .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        }
        httpPost.setConfig(configBuilder.build());
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

    public static String generateGetParams(String url,Map<String,String> params){
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
        String url="https://gny.ly.com/list?src=%E5%8C%97%E4%BA%AC&dest=%E4%BA%91%E5%8D%97&prop=1";
        Map<String,String> headers=new HashMap<>();
        headers.put("Cookie","dj-meta=%257B%2522ttf%2522%3A%2522100000001001110101101000101111000011010101110000101%257C10001010100110010010001%2522%2C%2522tz%2522%3A-480%2C%2522au%2522%3A%252248000_2_1_0_2_explicit_speakers%2522%2C%2522gp%2522%3A%2522Google%2520Inc.%40ANGLE%2520(NVIDIA%2520GeForce%2520GT%2520705%2520Direct3D11%2520vs_5_0%2520ps_5_0)%2522%2C%2522cv%2522%3A%252275ef4ff7dba805b5200bfb170dd6ceaed666c140%2522%2C%2522pls%2522%3A%2522Chrome%2520PDF%2520PluginChrome%2520PDF%2520ViewerNative%2520Client%2522%2C%2522hd%2522%3A%2522zh-CN_zh_4%2522%2C%2522sc%2522%3A%2522900_1600_24_1%2522%2C%2522ua%2522%3A%2522Mozilla%2F5.0%2520(Macintosh%3B%2520Intel%2520Mac%2520OS%2520X%252010_9_3)%2520AppleWebKit%2F537.75.14%2520(KHTML%2C%2520like%2520Gecko)%2520Version%2F7.0.3%2520Safari%2F7046A194A%2522%2C%2522ft%2522%3A%2522a0d3a5ae529422a5960d9975b42f14e673c6a992%2522%2C%2522lg%2522%3A%2522b0b257f74eb224153921aa5e43565375f0cebd8c%2522%257D");
        List<Cookie> cookieList=getDoGetCookie(url,null,headers);
        for(Cookie cookie:cookieList){
            System.out.println(cookie.toString());
        }
    }
}
