package dev.revere.judas.engine.command.input;

/**
 * Parsed view of one token that may represent an option alias.
 *
 * <p>Examples:
 * {@code --mode} -> key only,
 * {@code --mode=ranked} -> key + inline value.
 */
public final class ParsedOptionToken {
    private final String key;
    private final String inlineValue;

    private ParsedOptionToken(String key, String inlineValue) {
        this.key = key;
        this.inlineValue = inlineValue;
    }

    /**
     * Parses a raw token into option-key and optional inline-value parts.
     *
     * @param token raw user token
     * @return parsed token view
     */
    public static ParsedOptionToken parse(String token) {
        int equalsAt = token.indexOf('=');
        if (equalsAt > 0) {
            return new ParsedOptionToken(token.substring(0, equalsAt), token.substring(equalsAt + 1));
        }
        return new ParsedOptionToken(token, null);
    }

    /**
     * Returns the option-key segment used for alias lookup.
     *
     * @return option lookup key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the inline option payload.
     *
     * @return inline option value, or {@code null} when no inline payload is present
     */
    public String getInlineValue() {
        return inlineValue;
    }

    /**
     * Indicates whether this token used {@code key=value} syntax.
     *
     * @return {@code true} when an inline payload exists
     */
    public boolean hasInlineValue() {
        return inlineValue != null;
    }

    /**
     * Detects numeric-looking negative tokens so they are not misclassified as unknown options.
     *
     * <p>This allows values such as {@code -5} or {@code -2.5} to remain positional/typed values
     * instead of triggering option parsing branches.
     *
     * @param token raw user token
     * @return {@code true} when the token looks like a negative numeric literal rather than an option alias
     */
    public static boolean isLikelyNumeric(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '-') {
            return false;
        }
        boolean hasDigit = false;
        for (int i = 1; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch >= '0' && ch <= '9') {
                hasDigit = true;
                continue;
            }
            if (ch == '.') {
                continue;
            }
            return false;
        }
        return hasDigit;
    }
}
