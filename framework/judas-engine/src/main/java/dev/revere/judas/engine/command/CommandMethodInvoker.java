package dev.revere.judas.engine.command;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

import java.lang.reflect.InvocationTargetException;

/**
 * Performs reflective invocation of a resolved command handler.
 */
public class CommandMethodInvoker {

    /**
     * Invokes the resolved method on the root command instance.
     *
     * @param root owning root descriptor
     * @param methodDescriptor resolved method descriptor
     * @param invokeArgs invocation arguments in parameter order
     */
    public void invoke(CommandDescriptor root, CommandMethodDescriptor methodDescriptor, Object[] invokeArgs) {
        try {
            methodDescriptor.getMethod().setAccessible(true);
            methodDescriptor.getMethod().invoke(root.getInstance(), invokeArgs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("An error occurred while executing the command.", e);
        }
    }
}
