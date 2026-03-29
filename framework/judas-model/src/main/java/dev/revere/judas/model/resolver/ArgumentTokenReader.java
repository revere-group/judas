package dev.revere.judas.model.resolver;

/**
 * Shared helpers for reading the next token from an {@link ArgumentBuffer} during resolution.
 */
public final class ArgumentTokenReader {

    private ArgumentTokenReader() {
    }

    /**
     * Consumes the next token or throws {@link IllegalArgumentException} if none remain.
     *
     * @param context current resolver context
     * @return consumed token
     */
    public static String requireNext(ParameterResolveContext context) {
        ArgumentBuffer buffer = context.getArguments();
        if (!buffer.hasNext()) {
            throw new IllegalArgumentException("Missing argument: " + context.getParameter().getName());
        }
        return buffer.consume();
    }
}
