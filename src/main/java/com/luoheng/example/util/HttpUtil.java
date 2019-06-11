package com.luoheng.example.util;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUtil {
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

    public static Response doGet(String url,Map<String,String> params,Map<String,String> headers) throws IOException {
        client=getInstant();
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
            synchronized(HttpUtil.class){
                client=new OkHttpClient();
            }
        }
        return client;
    }
}
