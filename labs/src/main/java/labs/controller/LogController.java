package labs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import labs.aspect.LogExecution;
import labs.exception.ValidationException;
import labs.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logs")
@Validated
@LogExecution
@Tag(name = "Log controller", description = "API for downloading logs file")
public class LogController {
    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/{dateString}")
    @Operation(summary = "Get log file by date",
            description = "Returns log file, that contains logs for specified date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Log file was successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Logs for specified date weren't found")
    })
    public ResponseEntity<Resource> getLogFileByDate(@PathVariable @NotNull
            @Parameter(description = "The date in ISO format to get the logs file for",
                    example = "2025-04-01") String dateString)
            throws IOException {
        LocalDate date;
        try {
            date = LocalDate.parse(dateString);
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Passed string could not be parsed to date format \"yyyy-mm-dd\"");
        }
        Resource resource = logService.getLogFileByDate(date, null);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        resource.getFilename() + "\"").body(resource);
    }
}
