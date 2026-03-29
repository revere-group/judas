package dev.revere.judas.engine.command.input;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateful tokenizer for one command invocation argument list.
 *
 * <p>The processor walks tokens exactly once and classifies each token into one of:
 * positional argument, named option value, boolean flag, pending completion target, or option prefix candidate.
 * State is intentionally explicit ({@code waitingValueFor}, pending value fields) to keep control flow auditable.
 */
public final class CommandInputParseProcessor {
    private final CommandInputAliasIndex aliasIndex;
    private final boolean strictUnknownOption;
    private final boolean allowPendingValue;
    private final List<String> positional = new ArrayList<>();
    private final Map<ParameterDescriptor, String> namedValues = new LinkedHashMap<>();
    private final Set<ParameterDescriptor> flags = new LinkedHashSet<>();
    private ParameterDescriptor waitingValueFor;
    private ParameterDescriptor pendingValueParameter;
    private String pendingValuePrefix = "";
    private String optionPrefix;

    /**
     * Creates a token processor for one parsing mode.
     *
     * @param aliasIndex lookup index for option and flag aliases
     * @param strictUnknownOption whether unknown options should fail immediately
     * @param allowPendingValue whether completion-style pending value detection is enabled
     */
    public CommandInputParseProcessor(
            CommandInputAliasIndex aliasIndex,
            boolean strictUnknownOption,
            boolean allowPendingValue
    ) {
        this.aliasIndex = aliasIndex;
        this.strictUnknownOption = strictUnknownOption;
        this.allowPendingValue = allowPendingValue;
    }

    /**
     * Parses all tokens and returns the immutable parsed input snapshot.
     *
     * @param tokens raw user tokens in original order
     * @return parsed input representation for binding or completion
     */
    public ParsedCommandInput parse(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            this.processToken(tokens[i], i == tokens.length - 1);
        }
        this.finish();
        return new ParsedCommandInput(
                this.positional,
                this.namedValues,
                this.flags,
                this.pendingValueParameter,
                this.pendingValuePrefix,
                this.optionPrefix
        );
    }

    /**
     * Processes one token with awareness of whether it is the trailing token.
     *
     * @param token current raw token
     * @param lastToken whether this token is the final token in the input stream
     */
    private void processToken(String token, boolean lastToken) {
        if (this.consumeAwaitedValue(token, lastToken)) {
            return;
        }

        ParsedOptionToken parsedOption = ParsedOptionToken.parse(token);
        if (this.handleFlag(parsedOption)) {
            return;
        }
        if (this.handleValueOption(parsedOption, lastToken)) {
            return;
        }
        if (this.handleUnknownOptionToken(token, lastToken)) {
            return;
        }

        this.positional.add(token);
    }

    /**
     * Consumes a token as the value of a previously encountered named option.
     *
     * @param token current raw token
     * @param lastToken whether this token is the final token in the input stream
     * @return {@code true} when the token was consumed as an awaited value
     */
    private boolean consumeAwaitedValue(String token, boolean lastToken) {
        if (this.waitingValueFor == null) {
            return false;
        }
        if (this.allowPendingValue && lastToken) {
            this.pendingValueParameter = this.waitingValueFor;
            this.pendingValuePrefix = token;
            this.waitingValueFor = null;
            return true;
        }
        this.namedValues.put(this.waitingValueFor, token);
        this.waitingValueFor = null;
        return true;
    }

    /**
     * Handles boolean flag tokens.
     *
     * @param token parsed option token
     * @return {@code true} when the token matched a known flag alias
     */
    private boolean handleFlag(ParsedOptionToken token) {
        ParameterDescriptor parameter = this.aliasIndex.findFlagParameter(token.getKey());
        if (parameter == null) {
            return false;
        }
        this.flags.add(parameter);
        return true;
    }

    /**
     * Handles named value options (for example {@code --times 3} or {@code --times=3}).
     *
     * @param token parsed option token
     * @param lastToken whether this token is the final token in the input stream
     * @return {@code true} when the token matched a known value option alias
     */
    private boolean handleValueOption(ParsedOptionToken token, boolean lastToken) {
        ParameterDescriptor parameter = this.aliasIndex.findValueParameter(token.getKey());
        if (parameter == null) {
            return false;
        }
        if (token.hasInlineValue()) {
            this.consumeInlineOptionValue(parameter, token.getInlineValue(), lastToken);
            return true;
        }
        if (lastToken) {
            if (this.allowPendingValue) {
                this.pendingValueParameter = parameter;
                this.pendingValuePrefix = "";
                return true;
            }
            throw new CommandBindingException("Missing value for option: " + token.getKey());
        }
        this.waitingValueFor = parameter;
        return true;
    }

    /**
     * Consumes an inline value-option payload ({@code --name=value}).
     *
     * @param parameter matched value-option parameter
     * @param inlineValue inline value payload
     * @param lastToken whether this token is the final token in the input stream
     */
    private void consumeInlineOptionValue(ParameterDescriptor parameter, String inlineValue, boolean lastToken) {
        if (this.allowPendingValue && lastToken) {
            this.pendingValueParameter = parameter;
            this.pendingValuePrefix = inlineValue;
            return;
        }
        this.namedValues.put(parameter, inlineValue);
    }

    /**
     * Handles unknown option-like tokens based on current parsing mode.
     *
     * @param token current raw token
     * @param lastToken whether this token is the final token in the input stream
     * @return {@code true} when the token was consumed as a completion option-prefix candidate
     */
    private boolean handleUnknownOptionToken(String token, boolean lastToken) {
        if (!token.startsWith("-") || ParsedOptionToken.isLikelyNumeric(token)) {
            return false;
        }
        if (this.allowPendingValue && lastToken) {
            this.optionPrefix = token;
            return true;
        }
        if (this.strictUnknownOption) {
            throw new CommandBindingException("Unknown option: " + token);
        }
        return false;
    }

    /**
     * Finalizes parser state after all tokens are processed.
     *
     * <p>If a value option is still awaiting a payload, this method either captures pending completion state
     * (completion mode) or throws a binding exception (binding mode).
     */
    private void finish() {
        if (this.waitingValueFor == null) {
            return;
        }
        if (this.allowPendingValue) {
            this.pendingValueParameter = this.waitingValueFor;
            this.pendingValuePrefix = "";
            this.waitingValueFor = null;
            return;
        }
        throw new CommandBindingException("Missing value for option: " + firstAlias(this.waitingValueFor));
    }

    /**
     * Returns the primary alias used in "missing option value" error messages.
     *
     * @param parameter value-option parameter descriptor
     * @return first configured alias, or parameter name when no alias is configured
     */
    private static String firstAlias(ParameterDescriptor parameter) {
        String[] names = parameter.getOptionNames();
        if (names.length == 0) {
            return parameter.getName();
        }
        return names[0];
    }
}
