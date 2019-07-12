package com.luoheng.example._tuniu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bean{
    String productId;
    String productName;
    String productLink;
    String taName;
    List<Package> packageList=new ArrayList<>();
    List<String> pathList=new ArrayList<>();

    public Calendar newCalendar(){
        return new Calendar();
    }

    public Package newPackage(){
        return new Package();
    }

    public class Package{
        String name;
        Calendar calendar;

        public Calendar getCalendar(){
            return calendar;
        }

        public void setCalendar(Calendar calendar){
            this.calendar=calendar;
        }

        @Override
        public String toString(){
            return "Package{"+
                    "name='"+name+'\''+
                    ", calendar="+calendar+
                    '}';
        }
    }
    public class Calendar{
        String cityName;
        Map<String,String> map=new HashMap<>();

        @Override
        public String toString(){
            return "Calendar{"+
                    "cityName='"+cityName+'\''+
                    ", map="+map+
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
                ", packageList="+packageList+
                ", pathList="+pathList+
                '}';
    }

}
