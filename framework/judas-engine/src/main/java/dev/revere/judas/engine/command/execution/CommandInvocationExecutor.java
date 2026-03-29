package dev.revere.judas.engine.command.execution;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.middleware.CommandExecutionChain;
import dev.revere.judas.model.middleware.CommandExecutionContext;
import dev.revere.judas.model.middleware.CommandMiddleware;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.List;

/**
 * Executes a resolved handler through middleware, argument binding, and reflective invocation.
 */
public final class CommandInvocationExecutor {
    private final CommandExecutionServices executionServices;
    private final CommandArgumentBinder argumentBinder;
    private final CommandMethodInvoker methodInvoker;
    private final CommandConditionEvaluator conditionEvaluator;

    /**
     * Creates an invocation executor with middleware, binding, and condition collaborators.
     *
     * @param executionServices runtime services
     * @param argumentBinder argument binder
     * @param methodInvoker reflective invoker
     * @param conditionEvaluator condition evaluator used after binding
     */
    public CommandInvocationExecutor(
            CommandExecutionServices executionServices,
            CommandArgumentBinder argumentBinder,
            CommandMethodInvoker methodInvoker,
            CommandConditionEvaluator conditionEvaluator
    ) {
        this.executionServices = executionServices;
        this.argumentBinder = argumentBinder;
        this.methodInvoker = methodInvoker;
        this.conditionEvaluator = conditionEvaluator;
    }

    /**
     * Executes one resolved handler through middleware, binding, parameter checks, and invocation.
     *
     * @param descriptor root descriptor
     * @param methodDescriptor handler descriptor
     * @param context execution context
     * @param arguments raw argument buffer
     * @return handler response, or {@code null}
     */
    public Object execute(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            ArgumentBuffer arguments
    ) {
        CommandExecutionContext executionContext = new CommandExecutionContext(
                descriptor,
                methodDescriptor,
                context,
                arguments
        );
        List<CommandMiddleware> middlewares = this.executionServices.getMiddlewares();
        Object[] responseHolder = new Object[1];
        this.runMiddleware(0, middlewares, executionContext, new CommandExecutionChain() {
            @Override
            public void proceed() {
                Object[] invokeArgs = CommandInvocationExecutor.this.argumentBinder.bind(methodDescriptor, context, arguments);
                CommandInvocationExecutor.this.conditionEvaluator.evaluateParameters(
                        descriptor,
                        methodDescriptor,
                        context,
                        invokeArgs
                );
                responseHolder[0] = CommandInvocationExecutor.this.methodInvoker.invoke(descriptor, methodDescriptor, invokeArgs);
            }
        });
        return responseHolder[0];
    }

    private void runMiddleware(
            int index,
            List<CommandMiddleware> middlewares,
            CommandExecutionContext context,
            CommandExecutionChain terminal
    ) {
        if (index >= middlewares.size()) {
            terminal.proceed();
            return;
        }
        CommandMiddleware middleware = middlewares.get(index);
        middleware.handle(context, new CommandExecutionChain() {
            @Override
            public void proceed() {
                CommandInvocationExecutor.this.runMiddleware(index + 1, middlewares, context, terminal);
            }
        });
    }
}
