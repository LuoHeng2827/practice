package com.luoheng.example.test;

import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.LogUtil;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetTest implements Runnable{
    private static final String url="https://h5.m.taobao.com/trip/rx-search/travel-list/index.html?keyword=wuhan&fromSug=1&_wx_tpl=https%3A%2F%2Fh5.m.taobao.com%2Ftrip%2Frx-search%2Ftravel-list%2Findex.weex.js&globalSearchSource=sug_trip_scenic&nav=SCENIC&spm=181.8512603.x2112542.dHLItem-historyList-0-0&gsclickquery=wuhan&buyerLoc=%E5%8C%97%E4%BA%AC&ttid=seo.000000358&_projVer=0.1.90";

    private String requestData(){
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        headers.put("Accept","application/json");
        headers.put("Content-type","application/x-www-form-urlencoded");
        headers.put("Origin","https://h5.m.taobao.com");
        headers.put("Referer","https://h5.m.taobao.com/trip/rx-search/travel-list/index.html?keyword=wuhan&fromSug=1&_wx_tpl=https%3A%2F%2Fh5.m.taobao.com%2Ftrip%2Frx-search%2Ftravel-list%2Findex.weex.js&globalSearchSource=sug_trip_scenic&nav=SCENIC&spm=181.8512603.x2112542.dHLItem-historyList-0-0&gsclickquery=wuhan&buyerLoc=%E5%8C%97%E4%BA%AC&ttid=seo.000000358&_projVer=0.1.90");
        headers.put("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Mobile Safari/537.36");

        try{
            Response response= HttpUtil.doGet(url,params,headers);
            if(response.code()==200){
                String result=response.body().string();
                LogUtil.i(result);
                return result;
            }
            else{
                LogUtil.i("error: response code is "+response.code());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public void run() {
        requestData();
    }
    public static void main(String[] args){
        List<Thread> threadList=new ArrayList<>();
        for(int i=0;i<1;i++){
            GetTest getTest=new GetTest();
            threadList.add(new Thread(getTest));
        }
        for(int i=0;i<threadList.size();i++){
            threadList.get(i).start();
        }
    }
}
