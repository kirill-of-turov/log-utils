package com.github.kot.log.utils.bo;

import lombok.Data;

@Data
public class UrlData {

    private String urlPath;
    private int count;
    private int min;
    private int max;
    private int sum;
    private double average;
    private double median;

}
