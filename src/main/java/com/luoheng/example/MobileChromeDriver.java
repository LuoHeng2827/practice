package com.luoheng.example;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.HasTouchScreen;
import org.openqa.selenium.interactions.TouchScreen;
import org.openqa.selenium.remote.RemoteTouchScreen;

public class MobileChromeDriver extends ChromeDriver implements HasTouchScreen {
    private RemoteTouchScreen remoteTouchScreen;
    public MobileChromeDriver(ChromeOptions options) {
        super(options);
        remoteTouchScreen = new RemoteTouchScreen(getExecuteMethod());
    }

    @Override
    public TouchScreen getTouch() {
        return remoteTouchScreen;
    }
}
