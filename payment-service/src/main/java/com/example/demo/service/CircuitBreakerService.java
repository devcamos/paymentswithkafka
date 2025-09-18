package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker Service
 * Implements circuit breaker pattern for Kafka connectivity
 * Similar to Netflix Hystrix or Resilience4j
 */
@Service
public class CircuitBreakerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    // Circuit breaker states
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service is back
    }
    
    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    // Configuration
    private static final int FAILURE_THRESHOLD = 5;
    private static final long TIMEOUT_DURATION = 60000; // 1 minute
    private static final int SUCCESS_THRESHOLD = 3;
    
    /**
     * Check if Kafka is available (circuit breaker logic)
     */
    public boolean isKafkaAvailable() {
        switch (state) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime.get() > TIMEOUT_DURATION) {
                    state = State.HALF_OPEN;
                    logger.info("Circuit breaker transitioning to HALF_OPEN state");
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Record successful operation
     */
    public void recordSuccess() {
        if (state == State.HALF_OPEN) {
            int successCount = failureCount.decrementAndGet();
            if (successCount <= 0) {
                state = State.CLOSED;
                failureCount.set(0);
                logger.info("Circuit breaker transitioning to CLOSED state");
            }
        } else if (state == State.CLOSED) {
            failureCount.set(0);
        }
    }
    
    /**
     * Record failed operation
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            logger.warn("Circuit breaker transitioning to OPEN state from HALF_OPEN");
        } else if (state == State.CLOSED && failures >= FAILURE_THRESHOLD) {
            state = State.OPEN;
            logger.warn("Circuit breaker transitioning to OPEN state - failure threshold reached");
        }
    }
    
    /**
     * Get current circuit breaker state
     */
    public State getState() {
        return state;
    }
    
    /**
     * Get failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Reset circuit breaker (for testing)
     */
    public void reset() {
        state = State.CLOSED;
        failureCount.set(0);
        lastFailureTime.set(0);
        logger.info("Circuit breaker reset");
    }
}
