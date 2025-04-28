package service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import labs.exception.NotFoundException;
import labs.service.LogService;
import labs.service.impl.LogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {
    private Path currentDir;
    private LogService logService;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        logService = new LogServiceImpl();
        date = LocalDate.parse("2025-04-15");
        currentDir = Paths.get("./");
    }

    @Test
    void testGetLogFileByDate_WhenLogsExist() throws IOException {
        Path tempFile = Files.createTempFile(currentDir, "test_logs", ".log");
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, "2025-04-15 18:49:29 [main] INFO Message");

        Resource result = logService.getLogFileByDate(date, tempFile.toString());

        assertEquals("2025-04-15 18:49:29 [main] INFO Message\r\n",
                result.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void testGetLogFileByDate_WhenLogsNotExistForThisDay() throws IOException {
        Path tempFile = Files.createTempFile(currentDir, "test_logs", ".log");
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, "2024-07-10 10:27:03 [main] INFO Message");

        assertThrows(NotFoundException.class, () -> logService.getLogFileByDate(date, tempFile.toString()));
    }
}
