package dev.revere.judas.engine.command.input;

import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;

import java.util.List;

/**
 * Entry point for command-input token parsing.
 *
 * <p>This parser supports mixed positional arguments, unordered named options, and boolean flags.
 * The same parsing pipeline is reused for both execution and completion, with mode flags controlling
 * strictness for unknown options and support for pending trailing tokens.
 */
public final class CommandInputParser {

    private CommandInputParser() {
    }

    /**
     * Parses user input for command execution and argument binding.
     *
     * <p>Binding mode is strict:
     * unknown options fail fast and missing option values produce a {@code CommandBindingException}.
     *
     * @param tokens raw argument tokens as received from the command context
     * @param parameters target handler parameter descriptors
     * @return parsed token structure used by argument binding
     */
    public static ParsedCommandInput parseForBinding(String[] tokens, List<ParameterDescriptor> parameters) {
        return parse(tokens, parameters, true, false);
    }

    /**
     * Parses user input for completion planning.
     *
     * <p>Completion mode is permissive:
     * unknown trailing options are treated as prefix candidates and incomplete option values are represented as
     * pending value targets.
     *
     * @param tokens raw argument tokens as currently typed by the sender
     * @param parameters target handler parameter descriptors
     * @return parsed token structure used by completion planning
     */
    public static ParsedCommandInput parseForCompletion(String[] tokens, List<ParameterDescriptor> parameters) {
        return parse(tokens, parameters, false, true);
    }

    /**
     * Executes the shared parsing pipeline for one mode configuration.
     *
     * @param tokens raw argument tokens
     * @param parameters target handler parameter descriptors
     * @param strictUnknownOption whether unknown options should fail immediately
     * @param allowPendingValue whether trailing pending option values are tracked
     * @return parsed token structure
     */
    private static ParsedCommandInput parse(
            String[] tokens,
            List<ParameterDescriptor> parameters,
            boolean strictUnknownOption,
            boolean allowPendingValue
    ) {
        CommandInputAliasIndex aliasIndex = CommandInputAliasIndex.create(parameters);
        CommandInputParseProcessor processor = new CommandInputParseProcessor(
                aliasIndex,
                strictUnknownOption,
                allowPendingValue
        );
        return processor.parse(tokens);
    }
}
