package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.BasicCrawlerFactory;
import com.luoheng.example.lcrawler.CrawlerController;
import com.luoheng.example.util.PropertiesUtil;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DetailCrawlerFactory extends BasicCrawlerFactory<DetailCrawler>{
    private List<Proxy> proxyList=new ArrayList<>();
    public DetailCrawlerFactory(CrawlerController controller){
        super(controller);
        initProxy();
    }

    public DetailCrawlerFactory(CrawlerController controller, String name){
        super(controller, name);
        initProxy();
    }

    private void initProxy(){
        Gson gson=new Gson();
        String host=PropertiesUtil.getValue("proxy.host");
        String port=PropertiesUtil.getValue("proxy.port");
        String passwords=PropertiesUtil.getValue("proxy.passwords");
        Jedis jedis=new Jedis(host,Integer.parseInt(port));
        jedis.auth(passwords);
        String proxyJsonStr=jedis.get("proxy");
        JsonArray jsonArray=gson.fromJson(proxyJsonStr,JsonArray.class);
        for(int i=0;i<jsonArray.size();i++){
            JsonObject jsonObject=jsonArray.get(i).getAsJsonObject();
            Proxy proxy=new Proxy(Proxy.Type.HTTP,new InetSocketAddress(jsonObject.get("host").getAsString(),
                    jsonObject.get("port").getAsInt()));
            proxyList.add(proxy);
        }
    }

    @Override
    public DetailCrawler newInstance(){
        return new DetailCrawler(this);
    }

    @Override
    public Vector<DetailCrawler> newVector(int count){
        Vector<DetailCrawler> crawlerVector=new Vector<>();
        for(int i=0;i<count;i++){
            int proxyIndex=i%proxyList.size();
            DetailCrawler crawler=new DetailCrawler(this).proxy(proxyList.get(proxyIndex));
            crawlerVector.add(crawler);
        }
        return crawlerVector;
    }
}
