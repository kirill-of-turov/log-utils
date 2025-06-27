package com.github.kot.log.utils.bo;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static com.github.kot.log.utils.Runner.SPRING_TIMER_FILTER_PATTERN;

@Data
public class RequestData {

    private String method;
    private String urlPath;
    private int duration;

    public static List<RequestData> getFromSpringTimerFilterRecords(List<Record> springTimerFilterRecords) {
        List<RequestData> requestStatistics = new LinkedList<>();
        for (Record r : springTimerFilterRecords) {
            Matcher matcher = SPRING_TIMER_FILTER_PATTERN.matcher(r.getMessage());
            if (matcher.find()) {
                String m = matcher.group(1);
                String up = matcher.group(2);
                String d = matcher.group(3);
                RequestData requestData = new RequestData();
                requestData.setMethod(m);
                requestData.setUrlPath(up);
                requestData.setDuration(Integer.parseInt(d));
                requestStatistics.add(requestData);
            }
        }
        return requestStatistics;
    }

}