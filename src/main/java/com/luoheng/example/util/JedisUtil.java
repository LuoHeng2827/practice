package com.luoheng.example.util;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
public class JedisUtil {
    private static JedisPool jedisPool;
    static{
        initJedisPool();
    }

    public Jedis getResource(){
        return getInstance().getResource();
    }

    public void returnResource(Jedis jedis){
        if(jedis!=null)
            jedis.close();
    }

    private static void initJedisPool(){
        JedisPoolConfig config=new JedisPoolConfig();
        config.setMaxIdle(600);
        config.setMaxTotal(600);
        config.setMinEvictableIdleTimeMillis(60000L);
        config.setTimeBetweenEvictionRunsMillis(3000l);
        config.setNumTestsPerEvictionRun(-1);
        String passwords=PropertiesUtil.getValue("redis_passwords",null);
        String ip=PropertiesUtil.getValue("redis_ip","127.0.0.1");
        String port=PropertiesUtil.getValue("redis_port","6379");
        if(passwords!=null)
            jedisPool=new JedisPool(config,ip,Integer.parseInt(port),60000,passwords);
        else
            jedisPool=new JedisPool(config,ip,Integer.parseInt(port));
    }

    private JedisPool getInstance(){
        if(jedisPool==null){
            synchronized (JedisUtil.class){
                initJedisPool();
            }
        }
        return jedisPool;
    }



}
