package dev.revere.judas.runtime;

import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;

/**
 * Runtime-level customization options for framework-generated messages and help output.
 */
public final class CommandManagerOptions {
    private final CommandMessageProvider messageProvider;
    private final CommandHelpFormatter helpFormatter;
    private final String helpSubcommandName;
    private final boolean showUsageAfterBindingError;

    private CommandManagerOptions(Builder builder) {
        this.messageProvider = builder.messageProvider != null ? builder.messageProvider : new DefaultCommandMessageProvider();
        this.helpFormatter = builder.helpFormatter != null ? builder.helpFormatter : new DefaultCommandHelpFormatter();
        this.helpSubcommandName = normalizeHelpSubcommandName(builder.helpSubcommandName);
        this.showUsageAfterBindingError = builder.showUsageAfterBindingError;
    }

    /**
     * @return configured message provider
     */
    public CommandMessageProvider getMessageProvider() {
        return this.messageProvider;
    }

    /**
     * @return configured help formatter
     */
    public CommandHelpFormatter getHelpFormatter() {
        return this.helpFormatter;
    }

    /**
     * @return configured generated help subcommand name
     */
    public String getHelpSubcommandName() {
        return this.helpSubcommandName;
    }

    /**
     * @return {@code true} when subcommand usage should be appended after binding failures
     */
    public boolean isShowUsageAfterBindingError() {
        return this.showUsageAfterBindingError;
    }

    /**
     * @return new mutable builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static String normalizeHelpSubcommandName(String value) {
        String out = value == null ? "" : value.trim();
        if (out.isEmpty()) {
            return "help";
        }
        return out;
    }

    /**
     * Mutable builder for {@link CommandManagerOptions}.
     */
    public static final class Builder {
        private CommandMessageProvider messageProvider;
        private CommandHelpFormatter helpFormatter;
        private String helpSubcommandName = "help";
        private boolean showUsageAfterBindingError = true;

        /**
         * @param value custom message provider
         * @return this builder
         */
        public Builder messageProvider(CommandMessageProvider value) {
            this.messageProvider = value;
            return this;
        }

        /**
         * @param value custom help formatter
         * @return this builder
         */
        public Builder helpFormatter(CommandHelpFormatter value) {
            this.helpFormatter = value;
            return this;
        }

        /**
         * @param value generated help subcommand alias
         * @return this builder
         */
        public Builder helpSubcommandName(String value) {
            this.helpSubcommandName = value;
            return this;
        }

        /**
         * @param value whether to append concise usage after binding failures
         * @return this builder
         */
        public Builder showUsageAfterBindingError(boolean value) {
            this.showUsageAfterBindingError = value;
            return this;
        }

        /**
         * @return immutable options instance
         */
        public CommandManagerOptions build() {
            return new CommandManagerOptions(this);
        }
    }
}
