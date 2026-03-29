package dev.revere.judas.model.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describes a reflected method registered as a command or subcommand handler.
 */
public class CommandMethodDescriptor {
    private final String[] names;
    private final String permission;
    private final String description;
    private final boolean hidden;
    private final Method method;
    private final List<ParameterDescriptor> parameters;

    /**
     * @param names command or subcommand aliases
     * @param permission optional permission required for this handler
     * @param description optional human-readable description
     * @param hidden whether this handler should be hidden from help output
     * @param method reflected handler method
     * @param parameters parameter descriptors matching handler signature order
     */
    public CommandMethodDescriptor(
            String[] names,
            String permission,
            String description,
            boolean hidden,
            Method method,
            List<ParameterDescriptor> parameters
    ) {
        this.names = Arrays.copyOf(names, names.length);
        this.permission = permission;
        this.description = description;
        this.hidden = hidden;
        this.method = method;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    /**
     * @return handler aliases
     */
    public String[] getNames() {
        return Arrays.copyOf(names, names.length);
    }

    /**
     * @return permission node required by this handler, or {@code null}
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return human-readable description, or {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return {@code true} when this handler should be hidden in help output
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return reflected Java method backing this handler
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return immutable parameter descriptor list
     */
    public List<ParameterDescriptor> getParameters() {
        return parameters;
    }
}
