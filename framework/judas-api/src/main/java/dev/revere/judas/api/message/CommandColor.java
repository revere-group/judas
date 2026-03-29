package dev.revere.judas.api.message;

/**
 * Runtime-agnostic semantic message colors mapped by each platform adapter.
 */
public enum CommandColor {
    /** Clears any previously applied message styling. */
    RESET,
    /** Main accent color used for important but non-error content. */
    PRIMARY,
    /** Secondary accent color used for supporting details. */
    SECONDARY,
    /** Success-state feedback. */
    SUCCESS,
    /** Warning-state feedback. */
    WARNING,
    /** Error-state feedback. */
    ERROR,
    /** Neutral informational feedback. */
    INFO
}
