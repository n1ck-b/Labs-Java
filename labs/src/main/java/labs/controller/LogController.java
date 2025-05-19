package labs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import labs.aspect.CountVisits;
import labs.aspect.LogExecution;
import labs.exception.ValidationException;
import labs.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@CountVisits
@Controller
@RequestMapping("/logs")
@Validated
@LogExecution
@Tag(name = "Log controller", description = "API for downloading logs file")
@CrossOrigin(origins = "http://localhost:3000")
public class LogController {
    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/{dateString}")
    @Operation(summary = "Generate log file by date",
            description = "Returns ID of file, that will contain logs for " +
                    "specified date after the end of its generation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Log file generation has started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
    })
    public ResponseEntity<Map<String, Integer>> generateLogFileByDate(@PathVariable @NotNull
            @Parameter(description = "The date in ISO format to get the logs file for",
                example = "2025-04-01") String dateString) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Passed string could not be parsed to date format \"yyyy-mm-dd\"");
        }
        return new ResponseEntity<>(Map.of("taskId", logService.generateLogFileByDate(date, null)),
                HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Get status of file processing",
            description = "Returns status ('done' or 'processing')" +
                    " of processing log file with specified task ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status was got successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404",
                    description = "Processing file with specified task ID was not found")
    })
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, String>> getProcessingStatusByTaskId(@PathVariable @Positive @NotNull
                @Parameter(description = "Task ID to get file processing status by") Integer taskId) {
        return ResponseEntity.ok(Map.of("taskId", taskId.toString(),
                "status", logService.getFileProcessingStatus(taskId)));
    }

    @Operation(summary = "Download processed log file",
            description = "Returns file with logs for specified task ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File was successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Logs for specified date weren't found")
    })
    @GetMapping("/{taskId}/download")
    public ResponseEntity<Resource> downloadFileByTaskId(@PathVariable @Positive @NotNull
                @Parameter(description = "Task ID to get file by") Integer taskId) {
        Resource resource = logService.downloadFileByTaskId(taskId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                resource.getFilename() + "\"").body(resource);
    }
}
