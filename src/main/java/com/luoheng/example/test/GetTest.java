package com.luoheng.example.test;

import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.http.HttpClientUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetTest extends Thread{
    static String[] s1={"2939566","2568017","3001007","2660745","2839394","2548637"};
    static String[] s2={"5556394","2450295","5664016","2628844","2872357","2931944"};
    private int number;
    public GetTest(int number){
        this.number=number;
    }
    @Override
    public void run(){
        try{
            Map<String,String> params=new HashMap<>();
            Map<String,String> headers=new HashMap<>();
            //headers.put("host","www.mafengwo.cn");
            params.put("id",s1[number]);
            //headers.put("Referer","http://www.mafengwo.cn/sales/6066578.html");
            HttpResponse response=HttpClientUtil.doGet("http://www.mafengwo.cn/sales/detail/index/info",
                    params,headers,true,number);
            if(response.getStatusLine().getStatusCode()==200){
                params.put("id",s2[number]);
                System.out.println("request "+s1[number]+" ok");
                response=HttpClientUtil.doGet("http://www.mafengwo.cn/sales/detail/index/info",
                        params,headers,true,number);
                if(response.getStatusLine().getStatusCode()==200){
                    System.out.println("request "+s2[number]+" ok");
                }
                else{
                    System.out.println("error to request "+s2[number]+response.getStatusLine().getStatusCode());
                }
            }
            else{
                System.out.println("error to request "+s1[number]+response.getStatusLine().getStatusCode());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        /*List<Thread> threadList=new ArrayList<>();

        while(true){
            threadList.clear();
            for(int i=0;i<s1.length;i++){
                GetTest test=new GetTest(i);
                threadList.add(test);
            }
            for(Thread thread:threadList){
                if(!thread.isInterrupted()){
                    thread.start();
                }
            }
            ThreadUtil.waitMillis(5000);
        }*/
        Jedis jedis=JedisUtil.getResource();
        String taskData=jedis.rpop("list_mafengwo_product_id");
        while(taskData!=null){
            Map<String,String> params=new HashMap<>();
            Map<String,String> header=new HashMap<>();
            params.put("groupId",taskData);
            HttpResponse response=HttpClientUtil.doGet("http://www.mafengwo.cn/sales/detail/stock/info",params,null,true,1);
            if(response.getStatusLine().getStatusCode()==200){
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
            else{
                System.out.println("failed");
            }
            taskData=jedis.rpop("list_mafengwo_product_id");
        }
    }
}
