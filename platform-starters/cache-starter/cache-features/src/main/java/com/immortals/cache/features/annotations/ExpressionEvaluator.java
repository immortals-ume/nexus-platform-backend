package com.immortals.cache.features.annotations;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * Evaluates SpEL expressions for cache annotation conditions.
 * Supports condition and unless expressions with access to method parameters and results.
 *
 * @since 2.0.0
 */
public class ExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Evaluates a condition expression before method execution.
     * Used for @Cacheable condition and @CacheEvict condition.
     *
     * @param conditionExpression the SpEL expression to evaluate
     * @param method              the method being invoked
     * @param args                the method arguments
     * @param parameterNames      the parameter names
     * @return true if the condition is met, false otherwise
     */
    public boolean evaluateCondition(String conditionExpression, Method method,
                                     Object[] args, String[] parameterNames) {
        if (conditionExpression == null || conditionExpression.trim()
                .isEmpty()) {
            return true;
        }

        try {
            Expression expression = parser.parseExpression(conditionExpression);
            EvaluationContext context = createEvaluationContext(args, parameterNames, null);

            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to evaluate condition expression: " + conditionExpression +
                            " for method: " + method.getName(), e);
        }
    }

    /**
     * Evaluates an unless expression after method execution.
     * Used for @Cacheable unless and @CachePut unless.
     *
     * @param unlessExpression the SpEL expression to evaluate
     * @param method           the method being invoked
     * @param args             the method arguments
     * @param parameterNames   the parameter names
     * @param result           the method result
     * @return true if the unless condition is met (skip caching), false otherwise
     */
    public boolean evaluateUnless(String unlessExpression, Method method,
                                  Object[] args, String[] parameterNames, Object result) {
        if (unlessExpression == null || unlessExpression.trim()
                .isEmpty()) {
            return false;
        }

        try {
            Expression expression = parser.parseExpression(unlessExpression);
            EvaluationContext context = createEvaluationContext(args, parameterNames, result);

            Boolean unlessResult = expression.getValue(context, Boolean.class);
            return unlessResult != null && unlessResult;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to evaluate unless expression: " + unlessExpression +
                            " for method: " + method.getName(), e);
        }
    }

    /**
     * Creates an evaluation context with method parameters and optional result.
     *
     * @param args           the method arguments
     * @param parameterNames the parameter names
     * @param result         the method result (may be null for pre-execution evaluation)
     * @return the evaluation context
     */
    private EvaluationContext createEvaluationContext(Object[] args, String[] parameterNames, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        if (result != null) {
            context.setVariable("result", result);
            context.setRootObject(result);
        }

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }

            if (parameterNames != null && parameterNames.length == args.length) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
        }

        return context;
    }

    /**
     * Checks if caching should proceed based on condition and unless expressions.
     *
     * @param conditionExpression the condition expression (evaluated before method)
     * @param unlessExpression    the unless expression (evaluated after method)
     * @param method              the method being invoked
     * @param args                the method arguments
     * @param parameterNames      the parameter names
     * @param result              the method result
     * @return true if caching should proceed, false otherwise
     */
    public boolean shouldCache(String conditionExpression, String unlessExpression,
                               Method method, Object[] args, String[] parameterNames, Object result) {
        if (!evaluateCondition(conditionExpression, method, args, parameterNames)) {
            return false;
        }

        return !evaluateUnless(unlessExpression, method, args, parameterNames, result);
    }
}
