package com.github.kot.log.utils;

import com.github.kot.log.utils.bo.Record;
import com.github.kot.log.utils.bo.RequestData;
import com.github.kot.log.utils.bo.Summary;
import com.github.kot.log.utils.bo.UrlData;
import com.github.kot.log.utils.db.JdbcExecutionRepository;
import com.github.kot.log.utils.utils.EmailUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.kot.log.utils.Constants.DATE_TIME_FORMATTER_LOG;
import static java.lang.String.format;

@Slf4j
@Getter
@Setter
public class Runner {

    private static final String OPENDATA_VERSION_REGEXP = "Opendata \\[version=(\\S*)\\]";
    private static final Pattern OPENDATA_VERSION_PATTERN = Pattern.compile(OPENDATA_VERSION_REGEXP);

    private static final String LOG_RECORD_REGEXP = "(\\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2}:\\d{3})\\s+(\\w*) (\\[\\S*\\]) \\((\\w*).java:(\\d*)\\) - (.*)";
    private static final Pattern LOG_RECORD_PATTERN = Pattern.compile(LOG_RECORD_REGEXP);

    private static final String SPRING_TIMER_FILTER_REGEXP = "for Spring Action \\[(\\w*):([\\/.\\w]*)\\] took \\((\\d*)\\) ms";
    public static final Pattern SPRING_TIMER_FILTER_PATTERN = Pattern.compile(SPRING_TIMER_FILTER_REGEXP);
    private static final String RELEASE_REGEXP = "(\\d*\\.\\d*\\.\\d*(-\\d*)?)";

    private String logFilePath;
    private boolean clean;
    private String commit;
    private String userName;
    private String password;
    private String sender;
    private String recipients;

    private Summary summary;
    private ZonedDateTime zonedDateTime;

    public static void main(String[] args) {
        Runner runner = new Runner();
        runner.setLogFilePath(args[0]);
        runner.setClean(Boolean.parseBoolean(args[1]));
        runner.setCommit(args[2]);
        runner.setUserName(args[3]);
        runner.setPassword(args[4]);
        runner.setSender(args[5]);
        runner.setRecipients(args[6]);
        runner.run();
    }

    public void run() {
        zonedDateTime = ZonedDateTime.now();
        summary = new Summary();
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

            LinkedList<Record> records = new LinkedList<>();

            String message;

            for (String logLine : logLines) {
                Matcher matcher = LOG_RECORD_PATTERN.matcher(logLine);
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
                    String newMessage = lastMessage + System.lineSeparator() + logLine;
                    records.getLast().setMessage(newMessage);
                }
            }
            summary.setLogRecordsNumber(records.size());
            log.info("Number of records: {} ({} of all lines)", summary.getLogRecordsNumber(), getPercentage(summary.getRecordsToLinesRate()));

            String openDataVersionRecordMessage = records.stream().map(Record::getMessage).filter(OPENDATA_VERSION_PATTERN.asPredicate()).findFirst().orElseThrow(NoSuchElementException::new);

            Matcher opeDataVersionMatcher = OPENDATA_VERSION_PATTERN.matcher(openDataVersionRecordMessage);
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

            List<RequestData> requestStatistics = RequestData.getFromSpringTimerFilterRecords(springTimerFilterRecords);

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

            log.info("Satisfied: {} ({})", summary.getSatisfiedCount(), getPercentage(summary.getSatisfiedRecordRate()));
            log.info("Tolerant: {} ({})", summary.getTolerantCount(), getPercentage(summary.getTolerantRecordRate()));
            log.info("Frustrated: {} ({})", summary.getFrustratedCount(), getPercentage(summary.getFrustratedRecordRate()));
            log.info("Apdex: {}", getPercentage(summary.getApdex()));

            log.info("Current execution");
            log.info("{}", summary);

            // write request statistics by URL paths
            Set<String> urlPaths = requestStatistics.stream().map(RequestData::getUrlPath).collect(Collectors.toSet());
            List<UrlData> urlDataList = new ArrayList<>();
            for (String urlPath : urlPaths) {
                UrlData urlData = new UrlData();
                List<RequestData> urlSpecificRequestData = requestStatistics.stream().filter(rd -> rd.getUrlPath().equals(urlPath)).collect(Collectors.toList());
                int urlSpecificMinDuration = urlSpecificRequestData.stream().min(Comparator.comparing(RequestData::getDuration)).orElseThrow(NoSuchElementException::new).getDuration();
                int urlSpecificMaxDuration = urlSpecificRequestData.stream().max(Comparator.comparing(RequestData::getDuration)).orElseThrow(NoSuchElementException::new).getDuration();
                int urlSpecificSumDuration = urlSpecificRequestData.stream().mapToInt(RequestData::getDuration).sum();
                double urlSpecificAverageDuration = urlSpecificRequestData.stream().mapToInt(RequestData::getDuration).average().orElseThrow(NoSuchElementException::new);
                IntStream urlSpecificSortedDuration = urlSpecificRequestData.stream().mapToInt(RequestData::getDuration).sorted();
                double urlSpecificMedianDuration = urlSpecificRequestData.size() % 2 == 0 ?
                        urlSpecificSortedDuration.skip(urlSpecificRequestData.size() / 2L - 1).limit(2).average().orElseThrow(NoSuchElementException::new) :
                        urlSpecificSortedDuration.skip(urlSpecificRequestData.size() / 2).findFirst().orElseThrow(NoSuchElementException::new);

                urlData.setCount(urlSpecificRequestData.size());
                urlData.setUrlPath(urlPath);
                urlData.setMin(urlSpecificMinDuration);
                urlData.setMax(urlSpecificMaxDuration);
                urlData.setSum(urlSpecificSumDuration);
                urlData.setAverage(urlSpecificAverageDuration);
                urlData.setMedian(urlSpecificMedianDuration);

                log.info("{}", urlData);
                urlDataList.add(urlData);
            }

            String csvHeaders = "Path,Count,Min,Max,Sum,Avg,Mean";
            List<String> csvLines = urlDataList
                    .stream()
                    .map(ud -> String.format("%s,%d,%d,%d,%d,%f,%f",
                            ud.getUrlPath(),
                            ud.getCount(),
                            ud.getMin(),
                            ud.getMax(),
                            ud.getSum(),
                            ud.getAverage(),
                            ud.getMedian()))
                    .collect(Collectors.toList());
            List<String> csvAll = new ArrayList<>();
            csvAll.add(csvHeaders);
            csvAll.addAll(csvLines);
            Path file = Paths.get(String.format("url_paths_%s_%s.csv",
                    summary.getOdeeVersion(), zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))));
            try {
                Files.write(file, csvAll, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error(e.getLocalizedMessage(), e);
            }

            log.info("Top slowest responses");
            List<RequestData> topTen = requestStatistics
                    .stream()
                    .sorted(Comparator.comparingInt(RequestData::getDuration).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            topTen.forEach(requestData -> log.info("{}", requestData));

            JdbcExecutionRepository jdbcExecutionRepository = new JdbcExecutionRepository();
            List<Summary> allSummariesBefore = jdbcExecutionRepository.getExecutions();
            if (allSummariesBefore
                    .stream()
                    .noneMatch(s -> s.getServer().equals(summary.getServer())
                            && s.getOdeeVersion().equals(summary.getOdeeVersion())
                            && s.getStartTimestamp().isEqual(summary.getStartTimestamp()))) {
                jdbcExecutionRepository.addExecution(summary);
            }
            List<Summary> allSummariesAfter = jdbcExecutionRepository.getExecutions();

            List<Summary> processedSummaries = allSummariesAfter
                    .stream()
                    .filter(s -> (s.isClean()
                            || s.getId() == allSummariesAfter
                            .stream()
                            .max(Comparator.comparingLong(Summary::getId))
                            .orElseThrow(() -> new IllegalStateException("No executions found!"))
                            .getId()
                            || Pattern.matches(RELEASE_REGEXP, s.getOdeeVersion()))
                            && s.getId() > 0)
                    .sorted(Comparator.comparing(Summary::getId).reversed())
                    .limit(20)
                    .collect(Collectors.toList());

            log.info("Processed executions");
            processedSummaries.forEach(s -> log.info("{}", s));

            boolean showOthers = processedSummaries.stream().anyMatch(s -> s.getOtherLevelRecordsNumber() > 0);

            Map<String, Object> root = new HashMap<>();
            root.put("timestamp", zonedDateTime.toString());
            root.put("executions", processedSummaries);
            root.put("topTenSlowestResponses", topTen);
            root.put("lastExecution", summary);
            root.put("showOthers", showOthers);
            generateReports(root);

        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public String getPercentage(double fraction) {
        return format("%.02f", fraction * 100) + "%";
    }

    private void generateReports(Map<String, Object> root) {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setClassForTemplateLoading(Runner.class, "/templates/");
        configuration.setDefaultEncoding("UTF-8");

        String fileName = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".html";
        try {
            Template template = configuration.getTemplate("executions.ftl");
            StringWriter htmlWriter = new StringWriter();
            template.process(root, htmlWriter);
            template.process(root, new FileWriter(fileName));
            log.info("Send email report");
            EmailUtils.sendReport(summary.getOdeeVersion(), getUserName(), getPassword(), getSender(), getRecipients(), htmlWriter.toString(), zonedDateTime.toString());
        } catch (IOException | TemplateException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

}