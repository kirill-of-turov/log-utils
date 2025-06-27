package com.github.kot.log.utils;

import com.github.kot.log.utils.bo.Summary;
import com.github.kot.log.utils.db.JdbcExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class JdbcExecutionRepositoryTest {

    private static JdbcExecutionRepository jdbcExecutionRepository;

    @BeforeAll
    static void setup() {
        log.info("Initialize JDBC repository");
        jdbcExecutionRepository = new JdbcExecutionRepository();
    }

    @Test
    @DisplayName("Last execution id")
    void testLastExecutionId() {
        int lastId = jdbcExecutionRepository.getLastId();
        log.info("Last execution id: {}", lastId);
        assertTrue(lastId > 0, "Unexpected execution id!");
    }

    @Test
    @DisplayName("Clone last execution")
    void testCloneLastExecution() {
        int lastIdBefore = jdbcExecutionRepository.getLastId();
        List<Summary> summaries = jdbcExecutionRepository.getExecutions();
        Summary summary = summaries
                .stream()
                .max(Comparator.comparingLong(Summary::getId))
                .orElseThrow(() -> new IllegalStateException("No executions to clone!"));
        int numberOfAddedExecutions = jdbcExecutionRepository.addExecution(summary);
        int lastIdAfter = jdbcExecutionRepository.getLastId();
        assertEquals(numberOfAddedExecutions, lastIdAfter - lastIdBefore, "Unexpected last id!");
    }

}