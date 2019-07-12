package com.luoheng.example._tuniu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.redis.JedisUtil;
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

public class DBTaskCrawler extends Crawler{
    private static final String FROM_QUEUE="list_tuniu_db";
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

    private String buildJourneyString(JsonArray array){
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<array.size();i++){
            builder.append("D");
            builder.append(i+1);
            builder.append(":");
            JsonObject item=array.get(i).getAsJsonObject();
            builder.append(item.get("journeyDescription")
                    .getAsString().replaceAll("\\{.*\\}","-")
                    .replaceAll("\\(.*\\)","")
                    .replaceAll("（.*）",""));
        }
        return builder.toString();
    }

    private void saveData(JsonObject object){
        logger.info(object.get("productLink").getAsString());
        try{
            Connection connection=DBPoolUtil.getConnection();
            JsonArray journeyListResult=object.getAsJsonArray("journeyList");
            for(int i=0;i<journeyListResult.size();i++){
                JsonObject journeyResult=journeyListResult.get(i).getAsJsonObject();
                PreparedStatement preparedStatement=connection
                        .prepareStatement("INSERT INTO TUNIU_TRAVEL_PRODUCT_INFO("+
                        "`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`,`TA_NAME`," +
                                "`PACKAGE_NAME`,`TRAVEL_PLAN`,`TOUR_PARTY`,`PARYT_PALCE`,`TOUR_DESTN`," +
                                "`TOUR_DAYS`,`PROD_LINK`)" +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?);");
                String uuid=ThreadUtil.getUUID();
                preparedStatement.setString(1,uuid);
                preparedStatement.setInt(2,6);
                preparedStatement.setString(3,object.get("type").getAsString());
                preparedStatement.setString(4,object.get("productId").getAsString());
                preparedStatement.setString(5,object.get("name").getAsString());
                if(object.getAsJsonArray("cityList").size()==0){
                    preparedStatement.setString(6,"无");
                    preparedStatement.setString(7,"无");
                }
                else{
                    JsonObject firstCityItem=object.getAsJsonArray("cityList")
                            .get(0).getAsJsonObject();
                    preparedStatement.setString(6,firstCityItem.get("fullName").getAsString());
                    preparedStatement.setString(7,firstCityItem.get("companyName").getAsString());
                }
                preparedStatement.setString(8,buildJourneyString
                        (journeyResult.getAsJsonArray("journeyDetail")));
                String teamCityName=journeyResult.get("teamCityName").getAsString();
                String destination=journeyResult.get("destination").getAsString();
                preparedStatement.setInt(9,teamCityName.equals(destination)?2:1);
                preparedStatement.setString(10,teamCityName);
                preparedStatement.setString(11,destination);
                int day=journeyResult.get("dayDuration").getAsInt();
                int night=journeyResult.get("nightDuration").getAsInt();
                preparedStatement.setString(12,day+"天"+night+"晚");
                preparedStatement.setString(13,object.get("productLink").getAsString());
                preparedStatement.execute();
                preparedStatement.close();
                JsonArray cityListResult=object.getAsJsonArray("cityList");
                preparedStatement=connection.prepareStatement("INSERT INTO TUNIU_TRAVEL_PRODUCT_PRICE(" +
                        "`PROD_UNI_CODE`,`DATE`,`CITY`,`PRICE`) VALUES(?,?,?,?);");
                for(int j=0;j<cityListResult.size();j++){
                    JsonObject cityResult=cityListResult.get(j).getAsJsonObject();
                    JsonArray calendarResult=cityResult.getAsJsonArray("calendar");
                    String cityName=cityResult.get("cityName").getAsString();
                    for(int k=0;k<calendarResult.size();k++){
                        JsonObject calendarItem=calendarResult.get(k).getAsJsonObject();
                        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
                        Date date=new Date();
                        try{
                            date=format.parse(calendarItem.get("planDate").getAsString());
                        }catch(ParseException e){
                            e.printStackTrace();
                        }
                        double price=calendarItem.get("adultPrice").getAsDouble();
                        preparedStatement.setString(1,uuid);
                        preparedStatement.setDate(2,new java.sql.Date(date.getTime()));
                        preparedStatement.setString(3,cityName);
                        preparedStatement.setDouble(4,(double)Math.round(price*100)/100);
                        preparedStatement.addBatch();
                    }
                }
                int [] rs=preparedStatement.executeBatch();
                preparedStatement.close();
                logger.info(object.get("productLink").getAsString()+"-insert "+rs.length+"price data");
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
        saveData(gson.fromJson(taskData,JsonObject.class));
    }
}
