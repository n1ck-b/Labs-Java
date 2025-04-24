package labs.service;

import java.io.IOException;
import java.time.LocalDate;
import org.springframework.core.io.Resource;

public interface LogService {
    Resource getLogFileByDate(LocalDate date, String pathToLogFile) throws IOException;
}
