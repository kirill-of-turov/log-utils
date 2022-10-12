package com.github.kot.log.utils.bo;

import lombok.Data;

import java.time.Duration;
import java.time.ZonedDateTime;

import static java.lang.String.format;

@Data
public class Summary {

    private long id;

    private String server;
    private String odeeVersion;
    private boolean clean;
    private String commit;
    private int logLinesTotalNumber;
    private int logRecordsNumber;
    private int errorRecordsNumber;
    private int warnRecordsNumber;
    private int infoRecordsNumber;
    //    private int otherRecordsNumber;
    private int springTimerFilterRecordsNumber;
    private ZonedDateTime startTimestamp;
    private ZonedDateTime endTimestamp;
    //    private Duration duration;
    private int minResponseDuration;
    private int maxResponseDuration;
    private int sumResponseDuration;
    //    private double responseDurationPerDuration;
//    private double responseDurationPerDuration;
    private double averageResponseDuration;
    private double medianResponseDuration;
    private long satisfiedCount;
    private long tolerantCount;
//    private long frustratedCount;
//    private double apdex;

    public int getOtherLevelRecordsNumber() {
        return logRecordsNumber - errorRecordsNumber - warnRecordsNumber - infoRecordsNumber;
    }

    public double getRecordsToLinesRate() {
        return (double) logRecordsNumber / logLinesTotalNumber;
    }

    public double getErrorRecordRate() {
        return (double) errorRecordsNumber / logRecordsNumber;
    }

    public double getWarnRecordRate() {
        return (double) warnRecordsNumber / logRecordsNumber;
    }

    public double getInfoRecordRate() {
        return (double) infoRecordsNumber / logRecordsNumber;
    }

    public double getOtherRecordRate() {
        return (double) getOtherLevelRecordsNumber() / logRecordsNumber;
    }

    public double getSpringTimerFilterRecordRate() {
        return (double) springTimerFilterRecordsNumber / logRecordsNumber;
    }

    public Duration getLogDuration() {
        return Duration.between(startTimestamp, endTimestamp);
    }

    public String getLogDurationFormatted() {
        return format("%02d:%02d:%02d.%03d",
                getLogDuration().toHoursPart(),
                getLogDuration().toMinutesPart(),
                getLogDuration().toSecondsPart(),
                getLogDuration().toMillisPart());
    }


    public double getResponseDurationPerDuration() {
        return (double) sumResponseDuration / getLogDuration().toMillis();
    }

    public long getFrustratedCount() {
        return springTimerFilterRecordsNumber - satisfiedCount - tolerantCount;
    }

    public double getSatisfiedRecordRate() {
        return (double) satisfiedCount / springTimerFilterRecordsNumber;
    }

    public double getTolerantRecordRate() {
        return (double) tolerantCount / springTimerFilterRecordsNumber;
    }

    public double getFrustratedRecordRate() {
        return (double) getFrustratedCount() / springTimerFilterRecordsNumber;
    }

    public double getApdex() {
        return (satisfiedCount + 0.5 * tolerantCount) / springTimerFilterRecordsNumber;
    }

}