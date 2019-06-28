package com.luoheng.example.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeUtil{
    public static String unicodeToChinese(String unicode) {
        String unicodeCompile="\\\\u(.{4})?";
        String matchStr;
        Matcher matcher=Pattern.compile(unicodeCompile).matcher(unicode);
        while(matcher.find()){
            matchStr=matcher.group(1);
            unicode=unicode.replace("\\u"+matchStr,
                    String.valueOf((char)Integer.valueOf(matchStr,16).intValue()));
        }
        return unicode;
    }
    public static void main(String[] args) throws Exception{
        String s;
        File file=new File("./test.txt");
        BufferedReader reader=new BufferedReader(new FileReader(file));
        StringBuilder builder=new StringBuilder();
        String line=null;
        while((line=reader.readLine())!=null){
            builder.append(line);
        }

        System.out.println(unicodeToChinese(builder.toString()));
    }
}
