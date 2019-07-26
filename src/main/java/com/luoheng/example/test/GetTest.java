package com.luoheng.example.test;

import com.luoheng.example.util.http.HttpClientUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            headers.put("User-Agent","bykmbwjtnq");
            String url="https://gny.ly.com/list?prop=5&dest=云南&src=资阳&start=12";
            headers.put("cookie","dj-po=bd9fbb34dae50f0968355e672537d680;");
            HttpResponse response=HttpClientUtil.doGet(url,params,headers);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                logger.info(responseStr);
            }
            else{
                logger.info("code is "+code);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void test1() throws Exception{
        try{
            Map<String,String> params=new HashMap<>();
            Map<String,String> headers=new HashMap<>();
            headers.put("User-Agent","bykmbwjtnq");
            String url="https://gny.ly.com/list?prop=5&dest=云南&src=资阳&start=12";
            headers.put("cookie","dj-po=e47dc702429439e57222d9ac4ae9b76b;");
            HttpResponse response=HttpClientUtil.doGet(url,params,headers);
            int code=response.getStatusLine().getStatusCode();
            if(code==200){
                String responseStr=EntityUtils.toString(response.getEntity());
                logger.info(responseStr);
            }
            else{
                logger.info("code is "+code);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void test2() throws Exception{
        String dj_meta="dj-meta=%257B%2522ttf%2522%3A%25221110010010011011110011111111000001101101001001011000%257C11110011001011111010110%2522%2C%2522tz%2522%3A-480%2C%2522au%2522%3A%252248000_2_1_0_2_explicit_speakers%2522%2C%2522gp%2522%3A%2522Google%2520Inc.%40ANGLE%2520(NVIDIA%2520GeForce%2520GT%2520705%2520Direct3D11%2520vs_5_0%2520ps_5_0)%2522%2C%2522cv%2522%3A%252275ef4ff7dba805b5200bfb170dd6ceaed666c140%2522%2C%2522pls%2522%3A%2522Chrome%2520PDF%2520PluginChrome%2520PDF%2520ViewerNative%2520Client%2522%2C%2522hd%2522%3A%2522zh-CN_zh_4%2522%2C%2522sc%2522%3A%2522900_1600_24_1%2522%2C%2522ua%2522%3A%2522Mozilla%2F5.0%2520(Windows%2520NT%252010.0%3B%2520WOW64)%2520AppleWebKit%2F537.36%2520(KHTML%2C%2520like%2520Gecko)%2520Chrome%2F50.0.2661.87%2520Safari%2F537.36%2520OPR%2F37.0.2178.31%2522%2C%2522ft%2522%3A%2522a0d3a5ae529422a5960d9975b42f14e673c6a992%2522%2C%2522lg%2522%3A%2522b0b257f74eb224153921aa5e43565375f0cebd8c%2522%257D";
        String url="https://gny.ly.com/list?src=%E5%8C%97%E4%BA%AC&dest=%E4%BA%91%E5%8D%97&prop=1";
        Map<String,String> headers=new HashMap<>();
        headers.put("User-Agent","Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Mobile Safari/537.36");
        headers.put("Cookie",dj_meta);
        headers.put("Accept","*/*");
        headers.put("Host","gny.ly.com");
        headers.put("accept-encoding","gzip, deflate");
        headers.put("referer","https://gny.ly.com/list?src=%E5%8C%97%E4%BA%AC&dest=%E4%BA%91%E5%8D%97&prop=1");
        HttpResponse response=HttpClientUtil.doGet(url,null,headers,true,6);
        System.out.println(response.getStatusLine().getStatusCode());
        System.out.println(response.getAllHeaders().toString());
    }


    public static void main(String[] args) throws Exception{
        GetTest test=new GetTest(1);
        test.test1();
    }
}
