package dev.revere.judas.engine.command;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Computes tab-completion candidates for a registered command tree.
 */
public final class CommandCompletionService {

    private final CommandExecutionServices executionServices;

    /**
     * @param executionServices runtime services providing resolvers and suggestion providers
     */
    public CommandCompletionService(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Completes the next token for the given root descriptor and raw args.
     *
     * @param descriptor registered root descriptor
     * @param adapter platform completion adapter
     * @param args tokens after the command label
     * @return filtered completion list
     */
    public List<String> complete(CommandDescriptor descriptor, CompletionAdapter adapter, String[] args) {
        if (descriptor.getPermission() != null && !adapter.hasPermission(descriptor.getPermission())) {
            return Collections.emptyList();
        }

        String[] safe = args == null ? new String[0] : args;

        if (descriptor.getSubcommands().isEmpty()) {
            return this.completeForHandler(descriptor, descriptor.getDefaultMethod(), adapter, safe);
        }

        if (safe.length == 0) {
            return this.filterPrefix(this.collectSubcommandNames(descriptor), "");
        }

        String head = safe[0];
        CommandMethodDescriptor sub = this.findSubcommand(descriptor, head);
        if (sub == null) {
            return this.filterPrefix(this.collectSubcommandNames(descriptor), head);
        }

        if (sub.getPermission() != null && !adapter.hasPermission(sub.getPermission())) {
            return Collections.emptyList();
        }

        String[] rest = Arrays.copyOfRange(safe, 1, safe.length);
        return this.completeForHandler(descriptor, sub, adapter, rest);
    }

    private CommandMethodDescriptor findSubcommand(CommandDescriptor descriptor, String token) {
        for (CommandMethodDescriptor sub : descriptor.getSubcommands()) {
            for (String name : sub.getNames()) {
                if (name.equalsIgnoreCase(token)) {
                    return sub;
                }
            }
        }
        return null;
    }

    private List<String> collectSubcommandNames(CommandDescriptor descriptor) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (CommandMethodDescriptor sub : descriptor.getSubcommands()) {
            Collections.addAll(unique, sub.getNames());
        }
        if (descriptor.isGenerateHelp()) {
            unique.add(this.executionServices.getHelpSubcommandName());
        }
        return new ArrayList<>(unique);
    }

    private List<String> completeForHandler(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            CompletionAdapter adapter,
            String[] args
    ) {
        if (handler == null) {
            return Collections.emptyList();
        }

        List<ParameterDescriptor> consumable = this.consumableParameters(handler);
        if (consumable.isEmpty()) {
            return Collections.emptyList();
        }

        ParsedCommandInput parsedInput = CommandInputParser.parseForCompletion(args, consumable);

        if (parsedInput.getPendingValueParameter() != null) {
            return this.suggestionsForParameter(
                    root,
                    handler,
                    parsedInput.getPendingValueParameter(),
                    adapter,
                    parsedInput.getPendingValuePrefix(),
                    args
            );
        }

        if (parsedInput.getOptionPrefix() != null) {
            return this.filterPrefix(this.availableOptionAliases(consumable, parsedInput), parsedInput.getOptionPrefix());
        }

        List<ParameterDescriptor> positionalParameters = this.remainingPositionalParameters(consumable, parsedInput);
        if (positionalParameters.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> positionalTokens = parsedInput.getPositionalTokens();
        int n = positionalTokens.size();
        if (n == 0) {
            return this.suggestionsForParameter(root, handler, positionalParameters.get(0), adapter, "", args);
        }

        int max = positionalParameters.size();
        ParameterDescriptor last = positionalParameters.get(max - 1);
        if (n > max && !last.isConsumeRemaining()) {
            return Collections.emptyList();
        }

        ParameterDescriptor target = n > max ? last : positionalParameters.get(n - 1);
        String partial = positionalTokens.get(n - 1);
        return this.suggestionsForParameter(root, handler, target, adapter, partial, args);
    }

    private List<ParameterDescriptor> consumableParameters(CommandMethodDescriptor handler) {
        List<ParameterDescriptor> out = new ArrayList<>();
        for (ParameterDescriptor parameter : handler.getParameters()) {
            if (CommandContext.class.isAssignableFrom(parameter.getType())) {
                continue;
            }
            if (parameter.isSenderInjection()) {
                continue;
            }
            out.add(parameter);
        }
        return out;
    }

    private List<ParameterDescriptor> remainingPositionalParameters(
            List<ParameterDescriptor> parameters,
            ParsedCommandInput parsedInput
    ) {
        List<ParameterDescriptor> result = new ArrayList<>();
        for (ParameterDescriptor parameter : parameters) {
            if (parameter.isFlag()) {
                continue;
            }
            if (parsedInput.hasNamedValue(parameter)) {
                continue;
            }
            if (!parameter.isPositionalAllowed()) {
                continue;
            }
            result.add(parameter);
        }
        return result;
    }

    private List<String> availableOptionAliases(List<ParameterDescriptor> parameters, ParsedCommandInput parsedInput) {
        List<String> aliases = new ArrayList<>();
        for (ParameterDescriptor parameter : parameters) {
            if (parameter.isFlag() && parsedInput.hasFlag(parameter)) {
                continue;
            }
            if (!parameter.isFlag() && parsedInput.hasNamedValue(parameter)) {
                continue;
            }
            Collections.addAll(aliases, parameter.getOptionNames());
        }
        return aliases;
    }

    private List<String> suggestionsForParameter(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            ParameterDescriptor parameter,
            CompletionAdapter adapter,
            String partial,
            String[] tokensForHandler
    ) {
        if (parameter.getSuggestionProviderType() != null) {
            SuggestionProvider provider = this.executionServices.resolveSuggestionProvider(parameter.getSuggestionProviderType());
            CompletionContext context = new CompletionContext(
                    adapter.getSender(),
                    root,
                    handler,
                    parameter,
                    partial,
                    tokensForHandler
            );
            return this.filterPrefix(provider.suggest(context), partial);
        }

        ParameterResolver<?> resolver = this.executionServices.getResolver(parameter.getType());
        if (resolver == null) {
            return Collections.emptyList();
        }

        CompletionContext context = new CompletionContext(
                adapter.getSender(),
                root,
                handler,
                parameter,
                partial,
                tokensForHandler
        );
        return this.filterPrefix(resolver.complete(context), partial);
    }

    private List<String> filterPrefix(List<String> candidates, String partial) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (partial == null || partial.isEmpty()) {
            return new ArrayList<>(candidates);
        }
        String p = partial.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate != null && candidate.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(candidate);
            }
        }
        return out;
    }
}
