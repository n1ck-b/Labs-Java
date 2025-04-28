package labs.aspect;

import jakarta.servlet.http.HttpServletRequest;
import labs.dao.CacheItem;
import labs.service.VisitCounterService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AspectComponent {
    private final Logger logger = LoggerFactory.getLogger(AspectComponent.class);

    private final HttpServletRequest httpRequest;
    private final VisitCounterService visitCounterService;

    @Autowired
    public AspectComponent(HttpServletRequest request, VisitCounterService visitCounterService) {
        this.httpRequest = request;
        this.visitCounterService = visitCounterService;
    }

    @Before("@within(LogExecution)")
    public void logStartOfExecution(JoinPoint joinPoint) {
        logger.info("Method {} is executed with args {}",
                joinPoint.getSignature().toShortString(), joinPoint.getArgs());
    }

    @AfterReturning(pointcut = "@within(LogExecution)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String resultStr;
        if (result == null) {
            resultStr = null;
        } else {
            resultStr = result.toString();
        }
        logger.info("Method {} completed execution with result {}",
                joinPoint.getSignature().toShortString(), resultStr);
    }

    @AfterThrowing(pointcut = "execution(* labs..*(..))", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        logger.error("Method {} threw an exception {} with message '{}'",
                joinPoint.getSignature().toShortString(), ex.getClass().getSimpleName(), ex.getMessage());
    }

    @AfterReturning(pointcut = "execution(* labs.dao.SessionCache.addObject(..))", returning = "result")
    public void logAddingToCache(JoinPoint joinPoint, Object result) {
        CacheItem item = (CacheItem) result;
        logger.info("Add/update {} with key '{}' to/in cache",
                item.getObject().getClass().getSimpleName().toLowerCase(), item.getKey());
    }

    @Around("execution(* labs.dao.SessionCache.getObject(..))")
    public Object logGettingFromCache(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        Object object = joinPoint.proceed();
        if (object != null) {
            logger.info(String.format("Get %s with key '%s' from cache. Time elapsed = %.4fms",
                    object.getClass().getSimpleName().toLowerCase(),
                    joinPoint.getArgs()[0],
                    (System.nanoTime() - startTime) / 1000000.0));
        }
        return object;
    }

    @AfterReturning(pointcut = "execution(* labs.dao.SessionCache.removeObject(..))", returning = "result")
    public void logRemovingFromCache(JoinPoint joinPoint, Object result) {
        CacheItem item = (CacheItem) result;
        logger.info("Remove {} with key '{}' from cache",
                item.getObject().getClass().getSimpleName().toLowerCase(), item.getKey());
    }

    @Around("execution(* labs.service.impl.ProductServiceImpl.getProductsByQuery(..))")
    public Object logGettingProductsByQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object object = joinPoint.proceed();
        logger.info("Time elapsed for getting product by query = {}ms",
                System.currentTimeMillis() - startTime);
        return object;
    }

    @Around("execution(* labs.service.impl.ProductServiceImpl.getJsonFromExternalApi(..))")
    public Object logGettingJsonFromApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object object = joinPoint.proceed();
        logger.info("Time elapsed for API request = {}ms", System.currentTimeMillis() - startTime);
        return object;
    }

    @Pointcut("@annotation(CountVisits)")
    public void countVisitForEachMethod() { }

    @Pointcut("@within(CountVisits)")
    public void countVisitForClass() { }

    @Before("countVisitForEachMethod() || countVisitForClass()")
    public void countVisitsForUrl(JoinPoint joinPoint) {
        visitCounterService.incrementCounter(httpRequest.getRequestURI());
    }
}
