package com.github.kot.log.utils.db;

import com.github.kot.log.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import com.github.kot.log.utils.Summary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Properties;

@Slf4j
public class JdbcExecutionRepository {

    private static final String SPRING_DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String SPRING_DATASOURCE_USERNAME_PROPERTY = "spring.datasource.username";
    private static final String SPRING_DATASOURCE_PASSWORD_PROPERTY = "spring.datasource.password";
    private static final String SPRING_DATASOURCE_DRIVER_PROPERTY = "spring.datasource.driver";

    private final JdbcTemplate jdbcTemplate;

    public JdbcExecutionRepository() {
        Properties properties = new Properties();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("database.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        String datasourceUrl = System.getProperty(SPRING_DATASOURCE_URL_PROPERTY) == null
                ? properties.getProperty(SPRING_DATASOURCE_URL_PROPERTY)
                : System.getProperty(SPRING_DATASOURCE_URL_PROPERTY);
        log.info("Datasource URL: {}", datasourceUrl);
        driverManagerDataSource.setUrl(datasourceUrl);
        driverManagerDataSource.setUsername(properties.getProperty(SPRING_DATASOURCE_USERNAME_PROPERTY));
        driverManagerDataSource.setPassword(properties.getProperty(SPRING_DATASOURCE_PASSWORD_PROPERTY));
        driverManagerDataSource.setDriverClassName(properties.getProperty(SPRING_DATASOURCE_DRIVER_PROPERTY));
        jdbcTemplate = new JdbcTemplate(driverManagerDataSource);
    }

    public Integer getLastId() {
        return jdbcTemplate.queryForObject("SELECT MAX(ID) FROM dbo.EXECUTIONS;", Integer.class);
    }

    public List<Summary> getExecutions() {
        return jdbcTemplate.query(
                "SELECT TOP (1000) [ID], [SERVER], [ODEE_VERSION], [CLEAN], [LINES], [RECORDS], [ERRORS], [WARNS], [INFOS], [SPRING_TIMERS], [START], [END], [MIN], [MAX], [SUM], [AVERAGE], [MEDIAN], [SATISFIED], [TOLERANT] FROM [DEV].[dbo].[EXECUTIONS]",
                (rs, rowNum) -> {
                    Summary summary = new Summary();
                    summary.setId(rs.getLong("ID"));
                    summary.setServer(rs.getString("SERVER"));
                    summary.setOdeeVersion(rs.getString("ODEE_VERSION"));
                    summary.setClean(rs.getBoolean("CLEAN"));
                    summary.setLogLinesTotalNumber(rs.getInt("LINES"));
                    summary.setLogRecordsNumber(rs.getInt("RECORDS"));
                    summary.setErrorRecordsNumber(rs.getInt("ERRORS"));
                    summary.setWarnRecordsNumber(rs.getInt("WARNS"));
                    summary.setInfoRecordsNumber(rs.getInt("INFOS"));
                    summary.setSpringTimerFilterRecordsNumber(rs.getInt("SPRING_TIMERS"));
                    summary.setStartTimestamp(ZonedDateTime.parse(rs.getString("START"), Constants.DATE_TIME_FORMATTER_DB));
                    summary.setEndTimestamp(ZonedDateTime.parse(rs.getString("END"), Constants.DATE_TIME_FORMATTER_DB));
                    summary.setMinResponseDuration(rs.getInt("MIN"));
                    summary.setMaxResponseDuration(rs.getInt("MAX"));
                    summary.setSumResponseDuration(rs.getInt("SUM"));
                    summary.setAverageResponseDuration(rs.getDouble("AVERAGE"));
                    summary.setMedianResponseDuration(rs.getDouble("MEDIAN"));
                    summary.setSatisfiedCount(rs.getLong("SATISFIED"));
                    summary.setTolerantCount(rs.getLong("TOLERANT"));
                    return summary;
                }
        );
    }

    public int addExecution(Summary summary) {
        int id = getNextId();
        return jdbcTemplate.update(
                "INSERT INTO [DEV].[dbo].EXECUTIONS ([ID], [SERVER], [ODEE_VERSION], [CLEAN], [LINES], [RECORDS], [ERRORS], [WARNS], [INFOS], [SPRING_TIMERS], [START], [END], [MIN], [MAX], [SUM], [AVERAGE], [MEDIAN], [SATISFIED], [TOLERANT]) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                summary.getServer(),
                summary.getOdeeVersion(),
                summary.isClean(),
                summary.getLogLinesTotalNumber(),
                summary.getLogRecordsNumber(),
                summary.getErrorRecordsNumber(),
                summary.getWarnRecordsNumber(),
                summary.getInfoRecordsNumber(),
                summary.getSpringTimerFilterRecordsNumber(),
                summary.getStartTimestamp().format(Constants.DATE_TIME_FORMATTER_DB),
                summary.getEndTimestamp().format(Constants.DATE_TIME_FORMATTER_DB),
                summary.getMinResponseDuration(),
                summary.getMaxResponseDuration(),
                summary.getSumResponseDuration(),
                summary.getAverageResponseDuration(),
                summary.getMedianResponseDuration(),
                summary.getSatisfiedCount(),
                summary.getTolerantCount()
        );
    }

    private Integer getNextId() {
        Integer lastId = jdbcTemplate.queryForObject("SELECT MAX(ID) FROM dbo.EXECUTIONS;", Integer.class);
        if (lastId != null) {
            return lastId + 1;
        } else {
            return 1;
        }
    }

}