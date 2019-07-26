package com.luoheng.example.util;

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

    }
}
