package dev.revere.judas.model.resolver;

import java.util.Arrays;

/**
 * Mutable argument buffer used during command binding.
 */
public class ArgumentBuffer {
    private final String[] arguments;
    private int index;

    /**
     * @param arguments raw argument tokens
     */
    public ArgumentBuffer(String[] arguments) {
        this(arguments, 0);
    }

    private ArgumentBuffer(String[] arguments, int index) {
        this.arguments = arguments;
        this.index = index;
    }

    /**
     * @return {@code true} if at least one token remains
     */
    public boolean hasNext() {
        return index < arguments.length;
    }

    /**
     * @return next token without consuming it, or {@code null}
     */
    public String peek() {
        return this.hasNext() ? arguments[index] : null;
    }

    /**
     * @return next token and advance cursor, or {@code null}
     */
    public String consume() {
        if (!this.hasNext()) {
            return null;
        }

        return arguments[index++];
    }

    /**
     * @return number of unconsumed tokens
     */
    public int remainingCount() {
        return arguments.length - index;
    }

    /**
     * Consumes remaining tokens and joins them.
     *
     * @param delimiter delimiter inserted between consumed tokens
     * @return joined remaining tokens, or {@code null} when empty
     */
    public String consumeRemaining(String delimiter) {
        if (!this.hasNext()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        while (this.hasNext()) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(this.consume());
        }

        return builder.toString();
    }

    /**
     * @return shallow snapshot with identical cursor position
     */
    public ArgumentBuffer snapshot() {
        return new ArgumentBuffer(Arrays.copyOf(arguments, arguments.length), index);
    }
}
