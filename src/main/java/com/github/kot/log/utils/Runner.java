package com.github.kot.log.utils;

import com.github.kot.log.utils.db.JdbcExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.kot.log.utils.Constants.DATE_TIME_FORMATTER_LOG;
import static java.lang.String.format;

@Slf4j
public class Runner {

    public static void main(String[] args) {
        String logFilePath = args[0];
        boolean clean = Boolean.parseBoolean(args[1]);
        String commit = args[2];
        new Runner().run(logFilePath, clean, commit);
    }

    public void run(String logFilePath, boolean clean, String commit) {
        Summary summary = new Summary();
        summary.setClean(clean);
        summary.setCommit(commit);
        log.info("Clean execution: {}", summary.isClean());
        try {
            summary.setServer(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        try (FileInputStream inputStream = new FileInputStream(logFilePath)) {
            String logString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            List<String> logLines = Arrays.stream(logString.split("\r\n|\r|\n")).collect(Collectors.toList());
            summary.setLogLinesTotalNumber(logLines.size());
            log.info("Total number of lines: {}", summary.getLogLinesTotalNumber());

            Pattern logRecordPattern = Pattern.compile("(\\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2}:\\d{3})\\s+(\\w*) (\\[\\S*\\]) \\((\\w*).java:(\\d*)\\) - (.*)");
            LinkedList<Record> records = new LinkedList<>();

            String message;

            for (String logLine : logLines) {
                Matcher matcher = logRecordPattern.matcher(logLine);
                if (matcher.find()) {
                    String timestamp = matcher.group(1);
                    String level = matcher.group(2);
                    String thread = matcher.group(3);
                    String className = matcher.group(4);
                    String line = matcher.group(5);
                    message = matcher.group(6);
                    Record r = new Record();
                    LocalDateTime localDateTime = LocalDateTime.parse(timestamp, DATE_TIME_FORMATTER_LOG);
                    r.setTimestamp(ZonedDateTime.of(localDateTime, ZoneId.systemDefault()));
                    r.setLevel(level);
                    r.setThread(thread);
                    r.setClassName(className);
                    r.setLine(Integer.parseInt(line));
                    r.setMessage(message);
                    records.add(r);
                } else {
                    String lastMessage = records.getLast().getMessage();
                    String newMessage = lastMessage + System.getProperty("line.separator") + logLine;
                    records.getLast().setMessage(newMessage);
//                    log.info(records.getLast().getMessage());
                }
            }
            summary.setLogRecordsNumber(records.size());
            log.info("Number of records: {} ({} of all lines)", summary.getLogRecordsNumber(), getPercentage(summary.getRecordsToLinesRate()));

            Pattern opeDataVersionPattern = Pattern.compile("Opendata \\[version=(\\S*)\\]");
            String openDataVersionRecordMessage = records.stream().map(Record::getMessage).filter(opeDataVersionPattern.asPredicate()).findFirst().orElseThrow(NoSuchElementException::new);

            Matcher opeDataVersionMatcher = opeDataVersionPattern.matcher(openDataVersionRecordMessage);
            if (opeDataVersionMatcher.find()) {
                summary.setOdeeVersion(opeDataVersionMatcher.group(1));
                log.info("OpenData version: {}", summary.getOdeeVersion());
            }

            List<Record> errorRecords = records
                    .stream()
                    .filter(r -> r.getLevel().equals("ERROR"))
                    .collect(Collectors.toList());
            summary.setErrorRecordsNumber(errorRecords.size());
            log.info("ERROR records: {} ({} of all records)", summary.getErrorRecordsNumber(), getPercentage(summary.getErrorRecordRate()));

            List<Record> warnRecords = records
                    .stream()
                    .filter(r -> r.getLevel().equals("WARN"))
                    .collect(Collectors.toList());
            summary.setWarnRecordsNumber(warnRecords.size());
            log.info("WARN records: {} ({} of all records)", summary.getWarnRecordsNumber(), getPercentage(summary.getWarnRecordRate()));

            List<Record> infoRecords = records
                    .stream()
                    .filter(r -> r.getLevel().equals("INFO"))
                    .collect(Collectors.toList());
            summary.setInfoRecordsNumber(infoRecords.size());
            log.info("INFO records: {} ({} of all records)", summary.getInfoRecordsNumber(), getPercentage(summary.getInfoRecordRate()));

//            List<Record> otherLevelRecords = records
//                    .stream()
//                    .filter(r -> Arrays.stream(new String[]{"ERROR", "WARN", "INFO"}).noneMatch(r.getLevel()::equals))
//                    .collect(Collectors.toList());
            log.info("Other records: {} ({} of all records)", summary.getOtherLevelRecordsNumber(), getPercentage(summary.getOtherRecordRate()));

            List<Record> springTimerFilterRecords = records
                    .stream()
                    .filter(r -> r.getClassName().equals("SpringTimerFilter"))
                    .collect(Collectors.toList());
            summary.setSpringTimerFilterRecordsNumber(springTimerFilterRecords.size());
            log.info("Number of SpringTimerFilter records: {} ({} of all records)", summary.getSpringTimerFilterRecordsNumber(), getPercentage(summary.getSpringTimerFilterRecordRate()));

            Record firstRecord = records.stream().min(Comparator.comparing(Record::getTimestamp)).orElseThrow(NoSuchElementException::new);
            summary.setStartTimestamp(firstRecord.getTimestamp());
            Record lastRecord = records.stream().max(Comparator.comparing(Record::getTimestamp)).orElseThrow(NoSuchElementException::new);
            summary.setEndTimestamp(lastRecord.getTimestamp());

            log.info("Start: {}", firstRecord.getTimestamp());
            log.info("End: {}", lastRecord.getTimestamp());
            log.info("Duration: {} = {}", summary.getLogDuration(), format("%02d:%02d:%02d:%03d",
                    summary.getLogDuration().toHoursPart(),
                    summary.getLogDuration().toMinutesPart(),
                    summary.getLogDuration().toSecondsPart(),
                    summary.getLogDuration().toMillisPart()));

            List<RequestData> requestStatistics = new LinkedList<>();

            for (Record r : springTimerFilterRecords) {
                Pattern springTimerFilterPattern = Pattern.compile("for Spring Action \\[(\\w*):([\\/.\\w]*)\\] took \\((\\d*)\\) ms");
                Matcher matcher = springTimerFilterPattern.matcher(r.getMessage());
                if (matcher.find()) {
                    String method = matcher.group(1);
                    String urlPath = matcher.group(2);
                    String duration = matcher.group(3);
                    RequestData requestData = new RequestData();
                    requestData.setMethod(method);
                    requestData.setUrlPath(urlPath);
                    requestData.setDuration(Integer.parseInt(duration));
                    requestStatistics.add(requestData);
                }
            }

            int minDuration = requestStatistics.stream().min(Comparator.comparing(RequestData::getDuration)).orElseThrow(NoSuchElementException::new).getDuration();
            summary.setMinResponseDuration(minDuration);
            int maxDuration = requestStatistics.stream().max(Comparator.comparing(RequestData::getDuration)).orElseThrow(NoSuchElementException::new).getDuration();
            summary.setMaxResponseDuration(maxDuration);
            int sumDuration = requestStatistics.stream().mapToInt(RequestData::getDuration).sum();
            summary.setSumResponseDuration(sumDuration);
            double averageDuration = requestStatistics.stream().mapToInt(RequestData::getDuration).average().orElseThrow(NoSuchElementException::new);
            summary.setAverageResponseDuration(averageDuration);

            IntStream sortedDuration = requestStatistics.stream().mapToInt(RequestData::getDuration).sorted();
            double medianDuration = requestStatistics.size() % 2 == 0 ?
                    sortedDuration.skip(requestStatistics.size() / 2L - 1).limit(2).average().orElseThrow(NoSuchElementException::new) :
                    sortedDuration.skip(requestStatistics.size() / 2).findFirst().orElseThrow(NoSuchElementException::new);
            summary.setMedianResponseDuration(medianDuration);

            log.info("Min duration: {}", summary.getMinResponseDuration());
            log.info("Max duration: {}", summary.getMaxResponseDuration());
            log.info("Sum duration: {}", summary.getSumResponseDuration());
            log.info("Response duration per duration: {}", getPercentage(summary.getResponseDurationPerDuration()));
            log.info("Avg duration: {}", format("%.2f", summary.getAverageResponseDuration()));
            log.info("Median duration: {}", summary.getMedianResponseDuration());

            long satisfied = requestStatistics.stream().filter(requestData -> requestData.getDuration() <= 100).count();
            summary.setSatisfiedCount(satisfied);
            long tolerant = requestStatistics.stream().filter(requestData -> requestData.getDuration() > 100 && requestData.getDuration() <= 1000).count();
            summary.setTolerantCount(tolerant);
//            long frustrated = requestStatistics.stream().filter(requestData -> requestData.getDuration() > 1000).count();
//            summary.setFrustratedCount(frustrated);

            log.info("Satisfied: {} ({})", summary.getSatisfiedCount(), getPercentage(summary.getSatisfiedRecordRate()));
            log.info("Tolerant: {} ({})", summary.getTolerantCount(), getPercentage(summary.getTolerantRecordRate()));
            log.info("Frustrated: {} ({})", summary.getFrustratedCount(), getPercentage(summary.getFrustratedRecordRate()));
            log.info("Apdex: {}", getPercentage(summary.getApdex()));

            log.info("{}", summary);

            JdbcExecutionRepository jdbcExecutionRepository = new JdbcExecutionRepository();
            List<Summary> summaries = jdbcExecutionRepository.getExecutions();
            if (summaries
                    .stream()
                    .noneMatch(s -> s.getServer().equals(summary.getServer())
                            && s.getOdeeVersion().equals(summary.getOdeeVersion())
                            && s.getStartTimestamp().isEqual(summary.getStartTimestamp()))) {
                jdbcExecutionRepository.addExecution(summary);
            }

        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public String getPercentage(double fraction) {
        return format("%.02f", fraction * 100) + "%";
    }

}