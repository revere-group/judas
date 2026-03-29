package dev.revere.judas.model.completion;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Context passed to {@link dev.revere.judas.model.resolver.ParameterResolver#complete(CompletionContext)}
 * and {@link SuggestionProvider#suggest(CompletionContext)}.
 */
public final class CompletionContext {
    private final Object sender;
    private final CommandDescriptor root;
    private final CommandMethodDescriptor handler;
    private final ParameterDescriptor parameter;
    private final String partial;
    private final List<String> tokensAfterHandler;

    /**
     * @param sender platform sender
     * @param root root descriptor
     * @param handler active handler (root default or subcommand)
     * @param parameter parameter being completed
     * @param partial prefix of the token being completed
     * @param tokensAfterHandler tokens after the subcommand token, including the partial as the last segment
     */
    public CompletionContext(
            Object sender,
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            ParameterDescriptor parameter,
            String partial,
            String[] tokensAfterHandler
    ) {
        this.sender = sender;
        this.root = root;
        this.handler = handler;
        this.parameter = parameter;
        this.partial = partial != null ? partial : "";
        this.tokensAfterHandler = tokensAfterHandler == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(tokensAfterHandler, tokensAfterHandler.length)));
    }

    public Object getSender() {
        return this.sender;
    }

    public CommandDescriptor getRoot() {
        return this.root;
    }

    public CommandMethodDescriptor getHandler() {
        return this.handler;
    }

    public ParameterDescriptor getParameter() {
        return this.parameter;
    }

    /**
     * @return prefix of the token currently being completed (never {@code null})
     */
    public String getPartial() {
        return this.partial;
    }

    /**
     * @return arguments already typed after the subcommand token, including the partial as the last segment
     */
    public List<String> getTokensAfterHandler() {
        return this.tokensAfterHandler;
    }
}
