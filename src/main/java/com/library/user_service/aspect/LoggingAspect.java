package com.library.user_service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for logging method execution
 * 
 * This is a simple example of Aspect-Oriented Programming
 * It automatically logs:
 * - When a method starts
 * - How long it takes to execute
 * - When it completes
 * 
 * No need to add logging code in every method!
 */
@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    /**
     * This aspect runs "around" all methods in service and controller packages
     * 
     * The @Around annotation means:
     * - Code before joinPoint.proceed() runs BEFORE the method
     * - Code after joinPoint.proceed() runs AFTER the method
     * 
     * The execution(* com.library.user_service.service..*(..)) pattern means:
     * - * = any return type
     * - com.library.user_service.service.. = package and sub-packages
     * - * = any method name
     * - (..) = any parameters
     */
    @Around("execution(* com.library.user_service.service..*(..)) || " +
            "execution(* com.library.user_service.controller..*(..))")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        
        // Get method information
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        // Log before method execution
        logger.info("➡️  [{}] Calling method: {}", className, methodName);
        
        // Record start time
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        try {
            // Execute the actual method
            result = joinPoint.proceed();
            
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log after successful execution
            logger.info("✅ [{}] Method {} completed in {}ms", 
                       className, methodName, executionTime);
            
        } catch (Exception e) {
            // Calculate execution time even on error
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log if method throws exception
            logger.error("❌ [{}] Method {} failed after {}ms: {}", 
                        className, methodName, executionTime, e.getMessage());
            
            // Re-throw the exception so it can be handled normally
            throw e;
        }
        
        return result;
    }
}

