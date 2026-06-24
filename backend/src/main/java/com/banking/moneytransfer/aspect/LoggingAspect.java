package com.banking.moneytransfer.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Around("execution(* com.banking.moneytransfer.service..*(..)) || execution(* com.banking.moneytransfer.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());

        logger.info(">>> Entering: {}.{}() | Args: {}", className, methodName, args);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.info("<<< Exiting: {}.{}() | Time: {} ms | Result: {}", className, methodName, duration, result);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            logger.error("!!! Exception in {}.{}() | Time: {} ms | Error: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}
