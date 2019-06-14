package com.luoheng.example.util;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBPoolUtil{
    private static BasicDataSource dataSource=new BasicDataSource();
    static{
        dataSource.setDriverClassName(PropertiesUtil.getValue("jdbc.driverClassName"));
        dataSource.setUrl(PropertiesUtil.getValue("jdbc.url"));
        dataSource.setUsername(PropertiesUtil.getValue("jdbc.username"));
        dataSource.setPassword(PropertiesUtil.getValue("jdbc.passwords"));
        dataSource.setMaxTotal(Integer.parseInt(PropertiesUtil.getValue("jdbc.maxTotal")));
        dataSource.setMaxIdle(Integer.parseInt(PropertiesUtil.getValue("jdbc.maxIdle")));
        dataSource.setMaxConnLifetimeMillis(Long.parseLong(PropertiesUtil.getValue("jdbc.maxConnLifetimeMillis")));
        dataSource.setMaxWaitMillis(Long.parseLong(PropertiesUtil.getValue("jdbc.maxWaitMillis")));
    }

    public static BasicDataSource getDataSource(){
        return dataSource;
    }

    public static Connection getConnection() throws SQLException{
        return dataSource.getConnection();
    }
}
