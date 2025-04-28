package labs.controller;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import labs.aspect.LogExecution;
import labs.service.VisitCounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@LogExecution
@RestController
@RequestMapping("/counters")
public class VisitCounterController {
    private final VisitCounterService visitCounterService;

    @Autowired
    public VisitCounterController(VisitCounterService visitCounterService) {
        this.visitCounterService = visitCounterService;
    }

    @GetMapping()
    public ResponseEntity<Map<String, Long>> getCounterByUrl(@RequestParam(name = "url") String resourceUrl) {
        Long counter = visitCounterService.getCounterByUrl(resourceUrl);
        return ResponseEntity.ok(Map.of("visitsCounter", counter));
    }

    @GetMapping("/all-urls")
    public ResponseEntity<Map<String, AtomicLong>> getAllCounters() {
        return ResponseEntity.ok(visitCounterService.getAllCounters());
    }
}
