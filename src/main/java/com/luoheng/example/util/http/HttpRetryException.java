package com.luoheng.example.util.http;

public class HttpRetryException extends Exception{
    public HttpRetryException(){
        super();
    }

    public HttpRetryException(String message){
        super(message);
    }

    public HttpRetryException(String message, Throwable cause){
        super(message, cause);
    }

    public HttpRetryException(Throwable cause){
        super(cause);
    }
}
