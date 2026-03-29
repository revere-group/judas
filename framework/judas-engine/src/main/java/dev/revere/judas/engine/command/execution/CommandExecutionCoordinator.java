package dev.revere.judas.engine.command.execution;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.spi.CommandExecutionServices;

/**
 * Coordinates sync vs async invocation and response dispatch for one resolved handler.
 */
public final class CommandExecutionCoordinator {
    private final CommandExecutionServices executionServices;
    private final CommandInvocationExecutor invocationExecutor;
    private final CommandResponseDispatcher responseDispatcher;

    /**
     * Creates an execution coordinator with invocation and response collaborators.
     *
     * @param executionServices runtime services
     * @param invocationExecutor invocation executor
     * @param responseDispatcher response dispatcher
     */
    public CommandExecutionCoordinator(
            CommandExecutionServices executionServices,
            CommandInvocationExecutor invocationExecutor,
            CommandResponseDispatcher responseDispatcher
    ) {
        this.executionServices = executionServices;
        this.invocationExecutor = invocationExecutor;
        this.responseDispatcher = responseDispatcher;
    }

    /**
     * Runs one resolved command handler either synchronously or on the async executor.
     *
     * @param descriptor root descriptor
     * @param methodDescriptor handler descriptor
     * @param context execution context
     * @param arguments raw argument buffer
     * @param onFailure failure callback used by router for user-facing error handling
     */
    public void execute(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            ArgumentBuffer arguments,
            RunnableInvoker onFailure
    ) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Object response = CommandExecutionCoordinator.this.invocationExecutor.execute(
                            descriptor,
                            methodDescriptor,
                            context,
                            arguments
                    );
                    CommandExecutionCoordinator.this.responseDispatcher.dispatch(context, descriptor, methodDescriptor, response);
                } catch (RuntimeException exception) {
                    onFailure.run(exception);
                }
            }
        };
        if (methodDescriptor.isAsyncExecution()) {
            this.executionServices.getAsyncExecutor().execute(task);
            return;
        }
        task.run();
    }

    /**
     * Functional bridge so router can keep message policy while delegation stays reusable.
     */
    public interface RunnableInvoker {
        /**
         * Receives invocation failures so the router can handle user-facing messaging.
         *
         * @param exception execution failure
         */
        void run(RuntimeException exception);
    }
}
