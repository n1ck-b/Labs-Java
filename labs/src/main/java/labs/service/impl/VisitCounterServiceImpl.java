package labs.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import labs.aspect.LogExecution;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.service.VisitCounterService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@LogExecution
@Service
@Validated
public class VisitCounterServiceImpl implements VisitCounterService {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public long incrementCounter(String resourceUrl) {
        return counters.computeIfAbsent(resourceUrl, key -> new AtomicLong(0)).getAndIncrement();
    }

    @Override
    public long getCounterByUrl(String resourceUrl) {
        if (!counters.containsKey(resourceUrl)) {
            throw new NotFoundException(String.format(ExceptionMessages.COUNTER_NOT_FOUND, resourceUrl));
        }
        return counters.get(resourceUrl).get();
    }

    @Override
    public Map<String, AtomicLong> getAllCounters() {
        if (counters.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.COUNTERS_NOT_FOUND);
        }
        return counters;
    }
}
