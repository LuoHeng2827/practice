package com.luoheng.example.util.http;

import com.google.gson.Gson;
import com.luoheng.example.util.PropertiesUtil;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisConfig;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;

public class ProxyGetter extends Thread{
    private final static Logger logger = LogManager.getLogger(ProxyGetter.class);
    private static List<JedisConfig> jedisConfigList=new ArrayList<>();
    static{
        JedisConfig[] jedisConfigs={new JedisConfig("127.0.0.1",6379,"8085fbee2a31add7d363604402c9ea145d60e6f6f59ce9ca835cd0de5695602c")};
        jedisConfigList.addAll(Arrays.asList(jedisConfigs));
    }
    public static void main(String[] args)
    {
        ProxyGetter proxyGetter=new ProxyGetter();
        proxyGetter.start();
    }

    public void run() {
        while(true){
            core();
            ThreadUtil.waitMillis(15*1000);
        }
    }

    private void core(){
        try{
            List<HttpProxy> httpProxyList=getTask();
            if(httpProxyList.size()<10){
                //logger.info("代理信息获取失败-----------------------------------------------------------------------");
            }
            else{
                initAllProxyRedis(httpProxyList,jedisConfigList);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public List<HttpProxy> getTask() throws IOException{
        List<HttpProxy> httpProxyList=new ArrayList<>();
        String proxyUrl=PropertiesUtil.getValue("proxy.url");
        Map<String,String> headers=new HashMap<>();
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        HttpResponse response=HttpClientUtil.doGet(proxyUrl,null,headers);
        if(response.getStatusLine().getStatusCode()==200){
            String responseStr=EntityUtils.toString(response.getEntity());
            String[] ss=responseStr.split("\n");
            for(String s:ss){
                String[] proxy=s.split(":");
                HttpProxy httpProxy=new HttpProxy(proxy[0],Integer.parseInt(proxy[1].trim()));
                httpProxyList.add(httpProxy);
            }
        }
        return httpProxyList;
    }


    public void initAllProxyRedis(List<HttpProxy> httpProxyList,List<JedisConfig> jedisConfigList){
        Gson gson=new Gson();
        for(JedisConfig jedisConfig:jedisConfigList)
        {
            Jedis jedis=new Jedis(jedisConfig.host,jedisConfig.port);
            jedis.auth(jedisConfig.passwords);
            jedis.set("proxy",gson.toJson(httpProxyList));
            logger.info("-----正在对"+jedisConfig.host+"进行代理池的更新----");
        }
    }

}
