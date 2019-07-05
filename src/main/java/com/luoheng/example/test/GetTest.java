package com.luoheng.example.test;

import com.luoheng.example.util.http.HttpClientUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GetTest extends Thread{
    private int number;
    private Logger logger=LogManager.getLogger(GetTest.class);
    public GetTest(int number){
        this.number=number;
    }


    @Override
    public void run(){
        try{
            Map<String,String> params=new HashMap<>();
            Map<String,String> headers=new HashMap<>();
            String url="https://gny.ly.com/list?prop=1&dest=云南&src=北京";
            String cookie="dj-po=b445b98793a7a7d097f708578b3406d5;";
            headers.put("cookie",cookie);
            //headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
            HttpResponse response=HttpClientUtil.doGet(url,params,headers);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                Document document=Jsoup.parse(responseStr);
                if(document.getElementById("header")!=null){
                    logger.info("ok");
                }
                else{
                    logger.info("no");
                }
            }
            else{
                logger.info("code is "+code);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        for(int i=0;i<50;i++){
            GetTest test=new GetTest(0);
            test.start();
        }
    }
}
