package com.luoheng.example.xiecheng;

import java.util.ArrayList;
import java.util.List;

public class Bean{
    String productId;
    String productName;
    String productLink;
    String taName;
    Package bPackage;
    List<Price> priceList=new ArrayList<>();

    public Price newPrice(){
        return new Price();
    }

    public Package newPackage(){
        return new Package();
    }


    public class Package{
        String name;
        String id;
        String groupId;
        String cityName;
        String path;

        @Override
        public String toString(){
            return "Package{"+
                    "name='"+name+'\''+
                    ", id='"+id+'\''+
                    ", groupId='"+groupId+'\''+
                    ", cityName='"+cityName+'\''+
                    ", path='"+path+'\''+
                    '}';
        }
    }
    public class Price{
        String date;
        float price;

        @Override
        public String toString(){
            return "Price{"+
                    "date='"+date+'\''+
                    ", price="+price+
                    '}';
        }
    }

    @Override
    public String toString(){
        return "Bean{"+
                "productId='"+productId+'\''+
                ", productName='"+productName+'\''+
                ", productLink='"+productLink+'\''+
                ", taName='"+taName+'\''+
                ", bPackage="+bPackage+
                ", priceList="+priceList+
                '}';
    }
}
