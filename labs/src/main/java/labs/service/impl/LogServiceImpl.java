package labs.service.impl;

import jakarta.validation.constraints.PastOrPresent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import labs.aspect.LogExecution;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.service.LogService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
@LogExecution
public class LogServiceImpl implements LogService {
    private static final String LOG_FILE_PATH = "logs/app.log";
    private static final String DIR_PATH = "./logs";

    @Override
    public Resource getLogFileByDate(@PastOrPresent LocalDate date, String pathToLogFile) throws IOException {
        Path logFilePath;
        logFilePath = Paths.get(Objects.requireNonNullElse(pathToLogFile, LOG_FILE_PATH));
        List<String> allLines = Files.readAllLines(logFilePath);
        List<String> filteredStrings = allLines
                .stream()
                .filter(string -> string.startsWith(date.toString()))
                .collect(Collectors.toList());
        if (filteredStrings.isEmpty()) {
            throw new NotFoundException(String.format(ExceptionMessages.LOGS_NOT_FOUND, date.toString()));
        }
        Path pathOfNewLogFile = Files.createTempFile(Paths.get(DIR_PATH), "logs_" + date.toString(), ".log");
        Files.write(pathOfNewLogFile, filteredStrings);
        pathOfNewLogFile.toFile().deleteOnExit();
        return new UrlResource(pathOfNewLogFile.toUri());
    }
}
