package dev.revere.judas.engine.command.completion;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Public completion facade for a parsed command tree.
 *
 * <p>The service orchestrates root/subcommand routing checks, permission filtering, completion planning,
 * parameter suggestion resolution, and final prefix filtering.
 */
public final class CommandCompletionService {
    private final CommandExecutionServices executionServices;
    private final CommandCompletionPlanner completionPlanner;
    private final ParameterSuggestionResolver parameterSuggestionResolver;

    /**
     * Creates a completion service for one runtime manager instance.
     *
     * @param executionServices runtime services providing resolvers, suggestion providers, and help naming
     */
    public CommandCompletionService(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
        this.completionPlanner = new CommandCompletionPlanner();
        this.parameterSuggestionResolver = new ParameterSuggestionResolver(executionServices);
    }

    /**
     * Computes completion candidates for one root command and argument tail.
     *
     * <p>Behavior summary:
     * <ul>
     *     <li>Root permission is enforced first; handler (default/sub) permission before any handler completions.</li>
     *     <li>Subcommand name candidates omit aliases the sender cannot use (same for generated help context).</li>
     *     <li>Unknown first tokens are completed only from permitted subcommand aliases.</li>
     *     <li>Known handlers delegate target selection to {@link CommandCompletionPlanner}.</li>
     *     <li>Final candidate lists are prefix-filtered case-insensitively.</li>
     * </ul>
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
            return this.filterPrefix(this.collectSubcommandNames(descriptor, adapter), "");
        }

        String head = safe[0];
        CommandMethodDescriptor sub = this.findSubcommand(descriptor, head, adapter);
        if (sub == null) {
            return this.filterPrefix(this.collectSubcommandNames(descriptor, adapter), head);
        }

        String[] rest = Arrays.copyOfRange(safe, 1, safe.length);
        return this.completeForHandler(descriptor, sub, adapter, rest);
    }

    /**
     * Resolves a subcommand by first-token alias match for handlers the sender may use.
     *
     * @param descriptor root descriptor
     * @param token      first argument token
     * @param adapter    permission source
     * @return matching permitted subcommand descriptor, or {@code null} when unresolved or not permitted
     */
    private CommandMethodDescriptor findSubcommand(
            CommandDescriptor descriptor,
            String token,
            CompletionAdapter adapter
    ) {
        for (CommandMethodDescriptor sub : descriptor.getSubcommands()) {
            if (!this.mayCompleteHandler(adapter, sub)) {
                continue;
            }
            for (String name : sub.getNames()) {
                if (name.equalsIgnoreCase(token)) {
                    return sub;
                }
            }
        }
        return null;
    }

    /**
     * Collects subcommand aliases the sender may invoke, plus generated help when enabled.
     *
     * @param descriptor root descriptor
     * @param adapter    permission source
     * @return de-duplicated subcommand alias list in declaration order
     */
    private List<String> collectSubcommandNames(CommandDescriptor descriptor, CompletionAdapter adapter) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (CommandMethodDescriptor sub : descriptor.getSubcommands()) {
            if (!this.mayCompleteHandler(adapter, sub)) {
                continue;
            }
            Collections.addAll(unique, sub.getNames());
        }
        if (descriptor.isGenerateHelp()) {
            unique.add(this.executionServices.getHelpSubcommandName());
        }
        return new ArrayList<>(unique);
    }

    /**
     * @return {@code true} when the sender may receive completions for this handler (parameters, flags, etc.)
     */
    private boolean mayCompleteHandler(CompletionAdapter adapter, CommandMethodDescriptor handler) {
        if (handler == null) {
            return false;
        }
        String permission = handler.getPermission();
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }
        return adapter.hasPermission(permission);
    }

    /**
     * Completes tokens for one resolved handler.
     *
     * @param root root descriptor
     * @param handler resolved handler descriptor
     * @param adapter completion adapter
     * @param args handler-scoped argument tokens
     * @return filtered completion candidates
     */
    private List<String> completeForHandler(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            CompletionAdapter adapter,
            String[] args
    ) {
        if (!this.mayCompleteHandler(adapter, handler)) {
            return Collections.emptyList();
        }
        CommandCompletionPlan plan = this.completionPlanner.plan(handler, args);
        if (plan.targetsOptionAliases()) {
            return this.filterPrefix(plan.getOptionAliases(), plan.getPartial());
        }
        if (!plan.targetsParameter()) {
            return Collections.emptyList();
        }
        return this.filterPrefix(
                this.parameterSuggestionResolver.resolve(
                        root,
                        handler,
                        plan.getTargetParameter(),
                        adapter,
                        plan.getPartial(),
                        args
                ),
                plan.getPartial()
        );
    }

    /**
     * Applies case-insensitive prefix filtering to candidate lists.
     *
     * @param candidates raw candidate list
     * @param partial partial token prefix
     * @return filtered candidates preserving source order
     */
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
