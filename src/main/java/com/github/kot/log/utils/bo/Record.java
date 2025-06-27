package com.github.kot.log.utils.bo;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class Record {

    private ZonedDateTime timestamp;
    private String level;
    private String thread;
    private String className;
    private int line;
    private String message;

}