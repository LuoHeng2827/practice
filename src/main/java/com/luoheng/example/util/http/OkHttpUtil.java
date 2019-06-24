package com.luoheng.example.util.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.util.redis.JedisUtil;
import okhttp3.*;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil{
    private static OkHttpClient client;

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

    private static OkHttpClient buildProxy(Proxy proxy){
        OkHttpClient.Builder builder=new OkHttpClient.Builder();
        builder.proxy(proxy)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .callTimeout(30,TimeUnit.SECONDS);
        return builder.build();
    }

    public static Response doGetByProxy(String url,Map<String,String> params,
                                 Map<String,String> headers) throws IOException{
        Gson gson=new Gson();
        Jedis jedis=JedisUtil.getResource();
        String jsonStr=jedis.get("proxy");
        JsonArray jsonArray=gson.fromJson(jsonStr,JsonArray.class);
        Random random=new Random(new Date().getTime());
        int index=random.nextInt(jsonArray.size());
        JsonObject jsonObject=jsonArray.get(index).getAsJsonObject();
        HttpProxy httpProxy=new HttpProxy(jsonObject.get("host").getAsString(),jsonObject.get("port").getAsInt());
        return doGet(buildProxy(new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(httpProxy.host,httpProxy.port))),url,params,headers);
    }

    public static Response doGet(String url,Map<String,String> params,Map<String,String> headers)
            throws IOException{
        return doGet(getInstant(),url,params,headers);
    }

    private static Response doGet(OkHttpClient client,String url,Map<String,String> params,
                                  Map<String,String> headers) throws IOException {
        if(params==null)
            params=new HashMap<>();
        if(headers==null){
            headers=new HashMap<>();
        }
        url=generateGetParams(url, params);
        Request.Builder builder=new Request.Builder()
                .url(url)
                .get();
        for(Map.Entry<String,String> entry:headers.entrySet()){
            builder.addHeader(entry.getKey(),entry.getValue());
        }
        Request request=builder.build();
        return client.newCall(request).execute();
    }

    public static Response doRawPost(String url,Map<String,String> params,Map<String,String> headers,String raw)
            throws IOException{
        client=getInstant();
        if(params==null)
            params=new HashMap<>();
        if(headers==null){
            headers=new HashMap<>();
        }
        url=generateGetParams(url, params);
        RequestBody requestBody=RequestBody.create(MediaType.parse("application/json"),raw);
        Request.Builder builder=new Request.Builder()
                .url(url)
                .post(requestBody);
        for(Map.Entry<String,String> entry:headers.entrySet()){
            builder.addHeader(entry.getKey(),entry.getValue());
        }
        Request request=builder.build();
        return client.newCall(request).execute();
    }

    public static void doImageFormPost(String url, Map<String, String> formParams, String filesKey,
                                       List<File> fileList, Callback callback){
        client=getInstant();
        if(formParams==null)
            formParams=new HashMap<>();
        if(fileList==null)
            fileList=new ArrayList<>();
        if(filesKey ==null)
            filesKey="images";
        MultipartBody.Builder multipartBodyBuilder=new MultipartBody.Builder();
        for(String key:formParams.keySet()){
            multipartBodyBuilder.addFormDataPart(key,formParams.get(key));
        }
        for(File file:fileList){
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            multipartBodyBuilder.addFormDataPart(filesKey,file.getName(),requestBody);
        }
        Request request=new Request.Builder()
                .url(url)
                .post(multipartBodyBuilder.build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public static void doFormPost(String url,Map<String, String> formParams, Callback callback){
        client=getInstant();
        if(formParams==null)
            formParams=new HashMap<>();
        MultipartBody.Builder builder=new MultipartBody.Builder();
        for(Map.Entry<String,String> entry:formParams.entrySet()){
            builder.addFormDataPart(entry.getKey(),entry.getValue());
        }
        Request request=new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        client.newCall(request).enqueue(callback);
    }

    public static OkHttpClient getInstant(){
        if(client==null){
            synchronized(OkHttpUtil.class){
                OkHttpClient.Builder builder=new OkHttpClient.Builder();
                builder.connectTimeout(20,TimeUnit.SECONDS)
                        .readTimeout(20,TimeUnit.SECONDS)
                        .writeTimeout(20,TimeUnit.SECONDS);
                client=builder.build();
            }
        }
        return client;
    }
}
