package com.luoheng.example.mafengwo;

import com.google.gson.Gson;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.JedisUtil;
import com.luoheng.example.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DBTaskCrawler extends Crawler{
        private static final String FROM_QUEUE="list_mafengwo_tour_db";
    private Gson gson=new Gson();
    private Logger logger=LogManager.getLogger(DBTaskCrawler.class);
    public DBTaskCrawler(CrawlerFactory factory){
        super(factory);
    }

    public DBTaskCrawler(CrawlerFactory factory, String name){
        super(factory, name);
    }

    public DBTaskCrawler(CrawlerFactory factory, String name, long crawlInterval){
        super(factory, name, crawlInterval);
    }

    private void saveData(Bean bean){
        logger.info(bean.productLink);
        try{
            Connection connection=DBPoolUtil.getConnection();
            for(int i=0;i<bean.packageList.size();i++){
                Bean.Package bPackage=bean.packageList.get(i);
                PreparedStatement preparedStatement=connection
                        .prepareStatement("INSERT INTO MAFENGWO_TRAVEL_PRODUCT_INFO(" +
                                "`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`," +
                                "`TA_NAME`,`PACKAGE_NAME`,`TRAVEL_PLAN`,`PROD_LINK`)" +
                                "VALUES(?,?,?,?,?,?,?,?,?);");
                String uuid=ThreadUtil.getUUID();
                preparedStatement.setString(1,uuid);
                preparedStatement.setInt(2,6);
                preparedStatement.setString(3,"1");
                preparedStatement.setString(4,bean.productId);
                preparedStatement.setString(5,bean.productName);
                preparedStatement.setString(6,bean.taName);
                preparedStatement.setString(7,bPackage.name);
                if(bean.pathList.size()==0)
                    preparedStatement.setString(8,"no path");
                else if(bean.pathList.size()<i+1){
                    preparedStatement.setString(8, "unusual path");
                }
                else{
                    preparedStatement.setString(8, bean.pathList.get(i));
                }
                preparedStatement.setString(9,bean.productLink);
                preparedStatement.execute();
                preparedStatement.close();
                List<Bean.Calendar> calendarList=bPackage.calendarList;
                preparedStatement=connection.prepareStatement("INSERT INTO MAFENGWO_TRAVEL_PRODUCT_PRICE(" +
                        "`PROD_UNI_CODE`,`DATE`,`CITY`,`PRICE`) VALUES(?,?,?,?);");
                for(Bean.Calendar calendar:calendarList){
                    String cityName=calendar.cityName;
                    for(Map.Entry<String,String> entry:calendar.map.entrySet()){
                        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
                        Date date=new Date();
                        try{
                            date=format.parse(entry.getKey());
                        }catch(ParseException e){
                            e.printStackTrace();
                        }
                        double price=Double.parseDouble(entry.getValue());
                        preparedStatement.setString(1,uuid);
                        preparedStatement.setDate(2,new java.sql.Date(date.getTime()));
                        preparedStatement.setString(3,cityName);
                        preparedStatement.setDouble(4,(double)Math.round(price*100)/100);
                        preparedStatement.addBatch();
                    }
                }
                int [] rs=preparedStatement.executeBatch();
                preparedStatement.close();
                logger.info(bean.productLink+"-insert "+rs.length+"price data");
            }
            connection.close();

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public String getTaskData(){
        Jedis jedis=JedisUtil.getResource();
        String taskData=jedis.lpop(FROM_QUEUE);
        jedis.close();
        return taskData;
    }

    @Override
    public void crawl(String taskData){
        saveData(gson.fromJson(taskData,Bean.class));
    }
}
