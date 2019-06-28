package com.luoheng.example.util.redis;

import com.luoheng.example.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class JedisUtil {
    private static JedisPool jedisPool;
    private static JedisPoolConfig config;
    static{
        initJedisPool();
    }

    public static Jedis getResource(){
        return getInstance().getResource();
    }

    public void returnResource(Jedis jedis){
        if(jedis!=null)
            jedis.close();

    }

    private static void initJedisPool(){

        config=new JedisPoolConfig();
        config.setMaxWaitMillis(10000L);
        config.setMaxIdle(3000);
        config.setMaxTotal(9000);
        config.setMinEvictableIdleTimeMillis(60000L);
        config.setTimeBetweenEvictionRunsMillis(3000L);
        config.setNumTestsPerEvictionRun(-1);
        String passwords=PropertiesUtil.getValue("redis_passwords",null);
        String ip=PropertiesUtil.getValue("redis_ip","127.0.0.1");
        String port=PropertiesUtil.getValue("redis_port","6379");
        if(passwords!=null)
            jedisPool=new JedisPool(config,ip,Integer.parseInt(port),60000,passwords);
        else
            jedisPool=new JedisPool(config,ip,Integer.parseInt(port));
    }

    private static JedisPool getInstance(){
        if(jedisPool==null){
            synchronized (JedisUtil.class){
                initJedisPool();
            }
        }
        return jedisPool;
    }
    public static void lpush(String queueName,String... strings){
        Jedis jedis=getResource();
        jedis.lpush(queueName,strings);
        jedis.close();
    }

    public static void rpush(String queueName,String... strings){
        Jedis jedis=getResource();
        jedis.rpush(queueName,strings);
        jedis.close();
    }

    public static String lpop(String queueName){
        Jedis jedis=getResource();
        String data=jedis.lpop(queueName);
        jedis.close();
        return data;
    }

    public static String rpop(String queueName){
        Jedis jedis=getResource();
        String data=jedis.rpop(queueName);
        jedis.close();
        return data;
    }

    public static long llen(String queueName){
        Jedis jedis=getResource();
        long len=jedis.llen(queueName);
        jedis.close();
        return len;
    }

    public static String get(String key){
        Jedis jedis=getResource();
        String data=jedis.get(key);
        jedis.close();
        return data;
    }

    public static JedisPool getJedisPool() {
        return getInstance();
    }
}
