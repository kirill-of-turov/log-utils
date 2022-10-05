package com.github.kot.log.utils;

import com.github.kot.log.utils.db.JdbcExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class JavaVirtualMachineArgumentTest {

    @Test
    @DisplayName("Database server IP address argument")
    void testDatabaseServerIpAddressArgument() {
        String actualDatabaseServerIpAddress = new JdbcExecutionRepository().getDatabaseServerIpAddress();
        log.info("Datasource IP address: {}", actualDatabaseServerIpAddress);
        assertNotNull(actualDatabaseServerIpAddress);
    }

}