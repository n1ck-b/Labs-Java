package labs.service;

import java.time.LocalDate;
import org.springframework.core.io.Resource;

public interface LogService {
    int generateLogFileByDate(LocalDate date, String pathToLogFile);

    String getFileProcessingStatus(int taskId);

    Resource downloadFileByTaskId(int taskId);
}
