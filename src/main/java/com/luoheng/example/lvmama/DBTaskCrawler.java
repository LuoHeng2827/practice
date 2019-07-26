package com.luoheng.example.lvmama;

import com.google.gson.Gson;
import com.luoheng.example.lcrawler.Crawler;
import com.luoheng.example.lcrawler.CrawlerFactory;
import com.luoheng.example.util.BloomFilter.BFUtil;
import com.luoheng.example.util.CodeUtil;
import com.luoheng.example.util.DBPoolUtil;
import com.luoheng.example.util.ThreadUtil;
import com.luoheng.example.util.redis.JedisUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

//将产品的信息存储到数据库
public class DBTaskCrawler extends Crawler{
    public static final String FROM_QUEUE="lvmama_product_db";
    private Logger logger=LogManager.getLogger(DBTaskCrawler.class);
    private Gson gson=new Gson();
    public DBTaskCrawler(CrawlerFactory factory){
        super(factory);
    }

    public DBTaskCrawler(CrawlerFactory factory,String name){
        super(factory,name);
    }

    public DBTaskCrawler(CrawlerFactory factory,String name,long crawlInterval){
        super(factory,name,crawlInterval);
    }

    @Override
    public String getTaskData(){
        return JedisUtil.rpop(FROM_QUEUE);
    }
    private void saveData(String taskData){
        Bean bean=gson.fromJson(taskData,Bean.class);
        logger.info(bean.productLink);
        try{
            Connection connection=DBPoolUtil.getConnection();
            Bean.Package bPackage=bean.bPackage;
            List<Bean.Price> priceList=bean.priceList;
            String uuid=ThreadUtil.getUUID();
            PreparedStatement preparedStatement;
            if(!BFUtil.isExist(bean.productLink)){
                preparedStatement=connection
                        .prepareStatement("INSERT INTO LVMAMA_TRAVEL_PRODUCT_INFO(" +
                                "`PROD_UNI_CODE`,`OTA_ID`,`PROD_TYPE`,`OTA_PROD_ID`,`PROD_NAME`," +
                                "`TA_NAME`,`PACKAGE_NAME`,`TRAVEL_PLAN`,`PROD_LINK`)" +
                                "VALUES(?,?,?,?,?,?,?,?,?);");
                uuid=ThreadUtil.getUUID();
                preparedStatement.setString(1,uuid);
                preparedStatement.setInt(2,6);
                preparedStatement.setString(3,"1");
                preparedStatement.setString(4,bean.productId);
                preparedStatement.setString(5,bean.productName);
                preparedStatement.setString(6,bean.taName);
                preparedStatement.setString(7,bPackage.name);
                preparedStatement.setString(8,bPackage.path);
                preparedStatement.setString(9,CodeUtil.unicodeToChinese(bean.productLink));
                preparedStatement.execute();
                preparedStatement.close();
                BFUtil.add(bean.productLink);
            }
            else{
                preparedStatement=connection.prepareStatement("SELECT PROD_UNI_CODE FROM " +
                        " LVMAMA_TRAVEL_PRODUCT_INFO WHERE PROD_LINK=?");
                preparedStatement.setString(1,bean.productLink);
                ResultSet resultSet=preparedStatement.executeQuery();
                if(resultSet.next()){
                    uuid=resultSet.getString(1);
                }
                else{
                    Core.saveErrorMsg("uuid not exist");
                    System.exit(-1);
                }
            }
            preparedStatement=connection.prepareStatement("INSERT INTO LVMAMA_TRAVEL_PRODUCT_PRICE(" +
                    "`PROD_UNI_CODE`,`DATE`,`CITY`,`PRICE`) VALUES(?,?,?,?);");
            for(Bean.Price bPrice:priceList){
                preparedStatement.setString(1,uuid);
                SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
                Date date=new Date();
                try{
                    date=format.parse(bPrice.date);
                }catch(ParseException e){
                    e.printStackTrace();
                }
                preparedStatement.setDate(2,new java.sql.Date(date.getTime()));
                preparedStatement.setString(3,bPackage.cityName);
                preparedStatement.setDouble(4,(float)Math.round(bPrice.price*100)/100);
                preparedStatement.addBatch();
            }
            int res[]=preparedStatement.executeBatch();
            preparedStatement.close();
            logger.info(bean.productLink+"-insert "+res.length+"price data");
            connection.close();
        }catch(Exception e){
            saveFailureTask(taskData);
            Core.saveErrorMsg(e.getMessage());
            e.printStackTrace();
        }
    }
    private void saveFailureTask(String taskData){
        logger.info(taskData+" push to queue");
        JedisUtil.lpush(FROM_QUEUE,taskData);
    }

    @Override
    public void crawl(String taskData){
        saveData(taskData);
    }
}
