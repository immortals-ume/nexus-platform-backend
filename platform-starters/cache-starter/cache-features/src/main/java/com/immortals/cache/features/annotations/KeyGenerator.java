package com.immortals.cache.features.annotations;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generates cache keys from method parameters using SpEL expressions.
 * Supports dynamic key generation based on method arguments.
 *
 * @since 2.0.0
 */
public class KeyGenerator {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Generates a cache key from the given key expression and method context.
     *
     * @param keyExpression  the SpEL expression for the key
     * @param method         the method being invoked
     * @param args           the method arguments
     * @param parameterNames the parameter names
     * @return the generated cache key
     */
    public String generateKey(String keyExpression, Method method, Object[] args, String[] parameterNames) {
        if (keyExpression == null || keyExpression.trim()
                .isEmpty()) {
            return generateDefaultKey(method, args);
        }

        try {
            Expression expression = parser.parseExpression(keyExpression);
            EvaluationContext context = createEvaluationContext(args, parameterNames);

            Object keyValue = expression.getValue(context);
            return keyValue != null ? keyValue.toString() : "null";
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to evaluate key expression: " + keyExpression + " for method: " + method.getName(), e);
        }
    }

    /**
     * Generates a default cache key when no key expression is provided.
     * Uses the method name and all parameter values.
     *
     * @param method the method being invoked
     * @param args   the method arguments
     * @return the generated default key
     */
    public String generateDefaultKey(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return method.getName();
        }

        String argsKey = Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining(":"));

        return method.getName() + ":" + argsKey;
    }

    /**
     * Creates an evaluation context with method parameters.
     * Supports both named parameters (#paramName) and positional parameters (#p0, #p1, etc.).
     *
     * @param args           the method arguments
     * @param parameterNames the parameter names
     * @return the evaluation context
     */
    private EvaluationContext createEvaluationContext(Object[] args, String[] parameterNames) {
        StandardEvaluationContext context = new StandardEvaluationContext();

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
     * Generates a cache key with a namespace prefix.
     *
     * @param namespace      the cache namespace
     * @param keyExpression  the SpEL expression for the key
     * @param method         the method being invoked
     * @param args           the method arguments
     * @param parameterNames the parameter names
     * @return the generated cache key with namespace prefix
     */
    public String generateKeyWithNamespace(String namespace, String keyExpression,
                                           Method method, Object[] args, String[] parameterNames) {
        String key = generateKey(keyExpression, method, args, parameterNames);
        return namespace + ":" + key;
    }
}
