package com.luoheng.example.util.redis;

public class JedisConfig{
    public String host;
    public int port;
    public String passwords;
    public JedisConfig(String host,int port,String passwords){
        this.host=host;
        this.port=port;
        this.passwords=passwords;
    }
}
