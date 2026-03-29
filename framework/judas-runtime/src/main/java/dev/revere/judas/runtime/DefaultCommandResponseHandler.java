package dev.revere.judas.runtime;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.spi.CommandResponseHandler;

import java.lang.reflect.Array;

/**
 * Default response handler for return values from command methods.
 *
 * <p>Behavior:
 * iterables and arrays are flattened into one message per non-null element, while all other values are rendered
 * through {@link String#valueOf(Object)} and sent as a single message.
 */
public final class DefaultCommandResponseHandler implements CommandResponseHandler {
    @Override
    public boolean supports(Object response) {
        return response != null;
    }

    @Override
    public void handle(CommandContext context, CommandDescriptor root, CommandMethodDescriptor method, Object response) {
        if (response instanceof Iterable) {
            for (Object item : (Iterable<?>) response) {
                if (item != null) {
                    context.sendMessage(String.valueOf(item));
                }
            }
            return;
        }
        if (response.getClass().isArray()) {
            int length = Array.getLength(response);
            for (int i = 0; i < length; i++) {
                Object item = Array.get(response, i);
                if (item != null) {
                    context.sendMessage(String.valueOf(item));
                }
            }
            return;
        }
        context.sendMessage(String.valueOf(response));
    }
}
