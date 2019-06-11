package com.luoheng.example.lcrawler;

import java.util.Vector;

public interface CrawlerFactory<T extends Crawler> {
    T newInstance();

    Vector<T> newVector(int count);
}
