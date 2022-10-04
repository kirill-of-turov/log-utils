package com.github.kot.log.utils;

import lombok.Data;

@Data
public class RequestData {

    private String method;
    private String urlPath;
    private int duration;

}