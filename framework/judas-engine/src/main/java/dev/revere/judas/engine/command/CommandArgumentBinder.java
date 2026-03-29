package dev.revere.judas.engine.command;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.resolver.ParameterResolveContext;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts parsed user input into the method invocation arguments expected by a handler.
 *
 * <p>This binder intentionally handles resolution in phases:
 * <ol>
 *     <li>Inject framework objects ({@link CommandContext}, sender).</li>
 *     <li>Apply named options/flags from {@link ParsedCommandInput}.</li>
 *     <li>Consume remaining values positionally in declaration order.</li>
 * </ol>
 */
public class CommandArgumentBinder {
    private final CommandExecutionServices executionServices;

    /**
     * @param executionServices resolver and suggestion wiring from the runtime manager
     */
    public CommandArgumentBinder(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Produces invocation args for one handler execution.
     *
     * @param methodDescriptor target command method descriptor
     * @param context command execution context
     * @param arguments raw argument buffer from router
     * @return ordered invocation arguments for reflective method call
     */
    public Object[] bind(CommandMethodDescriptor methodDescriptor, CommandContext context, ArgumentBuffer arguments) {
        List<ParameterDescriptor> parameters = methodDescriptor.getParameters();
        Object[] invokeArgs = new Object[parameters.size()];
        ParsedCommandInput parsedInput = CommandInputParser.parseForBinding(
                this.consumeAll(arguments),
                parameters
        );

        List<String> positionalList = parsedInput.getPositionalTokens();
        String[] positionalTokens = new String[positionalList.size()];
        for (int i = 0; i < positionalList.size(); i++) {
            positionalTokens[i] = positionalList.get(i);
        }
        ArgumentBuffer positionalArguments = new ArgumentBuffer(positionalTokens);

        for (int i = 0; i < parameters.size(); i++) {
            ParameterDescriptor parameter = parameters.get(i);
            Class<?> type = parameter.getType();

            if (CommandContext.class.isAssignableFrom(type)) {
                invokeArgs[i] = context;
                continue;
            }

            if (parameter.isSenderInjection()) {
                invokeArgs[i] = this.resolveSender(parameter, context);
                continue;
            }

            if (parameter.isFlag()) {
                invokeArgs[i] = this.resolveFlag(parameter, context, parsedInput.hasFlag(parameter));
                continue;
            }

            if (parsedInput.hasNamedValue(parameter)) {
                invokeArgs[i] = this.resolveParameterFromNamedValue(
                        parameter,
                        context,
                        parsedInput.getNamedValue(parameter)
                );
                continue;
            }

            if (!parameter.isPositionalAllowed()) {
                if (parameter.isOptional() || parameter.getDefaultValue() != null) {
                    invokeArgs[i] = this.resolveParameter(parameter, context, new ArgumentBuffer(new String[0]));
                    continue;
                }
                throw new CommandBindingException("Missing required option: " + parameter.getOptionNames()[0]);
            }

            invokeArgs[i] = this.resolveParameter(parameter, context, positionalArguments);
        }

        if (positionalArguments.hasNext()) {
            throw new CommandBindingException("Too many arguments were provided.");
        }

        return invokeArgs;
    }

    private Object resolveSender(ParameterDescriptor parameter, CommandContext context) {
        Object sender = context.getSender();
        if (!parameter.getType().isInstance(sender)) {
            throw new CommandBindingException(
                    "Sender type mismatch for parameter '" + parameter.getName() + "' (required "
                            + parameter.getType().getSimpleName() + ")."
            );
        }
        return sender;
    }

    private Object resolveParameter(ParameterDescriptor parameter, CommandContext context, ArgumentBuffer arguments) {
        ParameterResolver<?> resolver = this.executionServices.getResolver(parameter.getType());
        if (resolver == null) {
            throw new CommandBindingException("No resolver found for parameter type: " + parameter.getType().getSimpleName());
        }

        if (!arguments.hasNext()) {
            if (!parameter.isOptional()) {
                throw new CommandBindingException("Missing argument: " + parameter.getName());
            }

            if (parameter.getDefaultValue() == null) {
                return null;
            }

            try {
                ArgumentBuffer fallback = new ArgumentBuffer(new String[]{parameter.getDefaultValue()});
                return resolver.resolve(new ParameterResolveContext(context, parameter, fallback));
            } catch (Exception exception) {
                throw new CommandBindingException("Invalid default value for parameter '" + parameter.getName() + "'.");
            }
        }

        try {
            ParameterResolveContext resolveContext;
            if (parameter.isConsumeRemaining()) {
                String remaining = arguments.consumeRemaining(" ");
                resolveContext = new ParameterResolveContext(context, parameter, new ArgumentBuffer(new String[]{remaining}));
            } else {
                resolveContext = new ParameterResolveContext(context, parameter, arguments);
            }

            return resolver.resolve(resolveContext);
        } catch (Exception exception) {
            throw new CommandBindingException("Invalid value for parameter '" + parameter.getName() + "'.");
        }
    }

    private Object resolveParameterFromNamedValue(ParameterDescriptor parameter, CommandContext context, String value) {
        ParameterResolver<?> resolver = this.executionServices.getResolver(parameter.getType());
        if (resolver == null) {
            throw new CommandBindingException("No resolver found for parameter type: " + parameter.getType().getSimpleName());
        }

        try {
            ArgumentBuffer buffer = new ArgumentBuffer(new String[]{value});
            return resolver.resolve(new ParameterResolveContext(context, parameter, buffer));
        } catch (Exception exception) {
            throw new CommandBindingException("Invalid value for option '" + parameter.getName() + "'.");
        }
    }

    private Object resolveFlag(ParameterDescriptor parameter, CommandContext context, boolean present) {
        if (present) {
            return Boolean.TRUE;
        }

        if (parameter.getDefaultValue() != null) {
            return this.resolveParameterFromNamedValue(parameter, context, parameter.getDefaultValue());
        }

        if (parameter.getType() == boolean.class) {
            return Boolean.FALSE;
        }

        return parameter.isOptional() ? null : Boolean.FALSE;
    }

    private String[] consumeAll(ArgumentBuffer arguments) {
        List<String> tokens = new ArrayList<>();
        while (arguments.hasNext()) {
            tokens.add(arguments.consume());
        }
        return tokens.toArray(new String[0]);
    }
}
