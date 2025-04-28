package labs.service.impl;

import jakarta.validation.constraints.PastOrPresent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import labs.aspect.LogExecution;
import labs.exception.ExceptionMessages;
import labs.exception.FileProcessingException;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@LogExecution
public class LogServiceImpl implements LogService {
    private static final String LOG_FILE_PATH = "logs/app.log";
    private static final String DIR_PATH = "./logs";

    private LogServiceImpl self;

    private final AtomicInteger currentTaskId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<UrlResource>> processingFiles = new ConcurrentHashMap<>();

    @Autowired
    public LogServiceImpl(@Lazy LogServiceImpl service) {
        this.self = service;
    }

    public LogServiceImpl() {}

    @Override
    public int generateLogFileByDate(@PastOrPresent LocalDate date, String pathToLogFile) {
        processingFiles.put(currentTaskId.get(), self.processLogFile(date, pathToLogFile));
        return currentTaskId.getAndIncrement();
    }

    @Async
    public CompletableFuture<UrlResource> processLogFile(LocalDate date, String pathToLogFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            Path logFilePath;
            logFilePath = Paths.get(Objects.requireNonNullElse(pathToLogFile, LOG_FILE_PATH));
            List<String> allLines;
            Path pathOfNewLogFile;
            try {
                allLines = Files.readAllLines(logFilePath);
            } catch (IOException ex) {
                throw new FileProcessingException("File with logs was not found");
            }
            List<String> filteredStrings = allLines
                    .stream()
                    .filter(string -> string.startsWith(date.toString()))
                    .collect(Collectors.toList());
            if (filteredStrings.isEmpty()) {
                throw new NotFoundException(String.format(ExceptionMessages.LOGS_NOT_FOUND, date.toString()));
            }
            try {
                pathOfNewLogFile = Files.createTempFile(Paths.get(DIR_PATH),
                        "logs_" + date.toString(), ".log");
                Files.write(pathOfNewLogFile, filteredStrings);
            } catch (IOException ex) {
                throw new FileProcessingException(ex.getMessage());
            }
            pathOfNewLogFile.toFile().deleteOnExit();
            try {
                return new UrlResource(pathOfNewLogFile.toUri());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String getFileProcessingStatus(int taskId) {
        if (!processingFiles.containsKey(taskId)) {
            throw new NotFoundException(String.format(ExceptionMessages.TASK_ID_NOT_FOUND, taskId));
        }
        if (processingFiles.get(taskId).isDone()) {
            return "done";
        } else {
            return "processing";
        }
    }

    @Override
    public Resource downloadFileByTaskId(int taskId) {
        if (!processingFiles.containsKey(taskId)) {
            throw new NotFoundException(String.format(ExceptionMessages.TASK_ID_NOT_FOUND, taskId));
        }
        if (!processingFiles.get(taskId).isDone()) {
            throw new ValidationException("Requested log file is still processing");
        }
        try {
            return processingFiles.get(taskId).get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
