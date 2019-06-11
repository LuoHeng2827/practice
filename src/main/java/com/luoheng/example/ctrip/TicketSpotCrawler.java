package com.luoheng.example.ctrip;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luoheng.example.util.HttpUtil;
import com.luoheng.example.util.LogUtil;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketSpotCrawler implements Runnable{
    //private static final String REQUEST_JSON="{\"pageid\":10320662472,\"searchtype\":1,\"districtid\":2,\"needfact\":false,\"sort\":1,\"pidx\":2,\"isintion\":true,\"psize\":20,\"imagesize\":\"C_190_190\",\"reltype\":1,\"assistfilter\":{\"userChooseSite\":\"145\"},\"spara\":\"\",\"filters\":[],\"excepts\":[],\"abtests\":[],\"contentType\":\"json\",\"head\":{\"cid\":\"09031089210204819718\",\"ctok\":\"\",\"cver\":\"1.0\",\"lang\":\"01\",\"sid\":\"8888\",\"syscode\":\"09\",\"auth\":\"\",\"extension\":[{\"name\":\"protocal\",\"value\":\"https\"}]},\"ver\":\"7.14.2\"}";
    private static final String REQUEST_JSON="{\"spotid\":65957,\"pageid\":10320662470,\"imgsize\":\"C_640_360\",\"contentType\":\"json\",\"head\":{\"cid\":\"09031089210204819718\",\"ctok\":\"\",\"cver\":\"1.0\",\"lang\":\"01\",\"sid\":\"8888\",\"syscode\":\"09\",\"auth\":\"\",\"extension\":[{\"name\":\"protocal\",\"value\":\"https\"}]},\"ver\":\"8.3.2\"}";
    //private static final String TARGET_URL="https://sec-m.ctrip.com/restapi/soa2/12530/json/ticketSpotSearch?_fxpcqlniredt=09031089210204819718";
    private static final String TARGET_URL="https://sec-m.ctrip.com/restapi/soa2/12530/json/scenicSpotDetails?_fxpcqlniredt=09031089210204819718";
    private Gson gson=new Gson();
    private String buildRequestJson(String cid,int pidx,int psize){
        JsonObject jsonObject=gson.fromJson(REQUEST_JSON,JsonObject.class);
        /*jsonObject.addProperty("pidx",pidx);
        jsonObject.addProperty("psize",psize);*/
        JsonObject head=jsonObject.getAsJsonObject("head");
        head.addProperty("cid",cid);
        return jsonObject.toString();
    }



    private String requestData(String cid,int pidx,int psize) throws IOException{
        Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();
        String requestJson=buildRequestJson(cid, pidx, psize);
        Response response= HttpUtil.doRawPost(TARGET_URL,params,headers,requestJson);
        if(response.code()==200){
            String result=response.body().string();
            LogUtil.i(result);
            response.close();
            return result;
        }
        else{
            LogUtil.e("response code:"+response.code());
            return null;
        }
    }
    @Override
    public void run() {
        try{
            requestData("09031089210204819718",1,1000);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void test(){
        String json=buildRequestJson("123",2,100);
        LogUtil.i(json);
    }
    public static void main(String[] args){
        List<Thread> threadList=new ArrayList<>();
        for(int i=0;i<600;i++){
            TicketSpotCrawler crawler=new TicketSpotCrawler();
            threadList.add(new Thread(crawler));
        }
        for(int i=0;i<threadList.size();i++){
            threadList.get(i).start();
        }
    }
}
