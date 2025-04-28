package labs.service;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public interface VisitCounterService {
    long incrementCounter(@NotNull String resourceUrl);

    long getCounterByUrl(@NotNull String resourceUrl);

    Map<String, AtomicLong> getAllCounters();
}
