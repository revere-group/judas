# Judas

Judas is a modular, annotation-driven command framework for the JVM. The **engine and runtime are platform-agnostic**; you plug in a `CommandManager` implementation for your host (game server, desktop app, bot, CLI, etc.). This repository ships **reference adapters**—`judas-bukkit` for Spigot/Bukkit and `judas-cli` for a terminal-style loop—plus examples that share the same core.

- **Java 8+**
- **Maven** multi-module build under `dev.revere.judas` (set the dependency `version` to match what you publish or consume)
- Holders extend `dev.revere.judas.model.command.BaseCommand`; registration and dispatch go through `dev.revere.judas.runtime.CommandManager` (subclass it per platform; see `BukkitCommandManager`, `CliCommandManager`).

---

## Modules

| Module | Role |
|--------|------|
| **judas-api** | Annotations (`@RootCommand`, `@Subcommand`, …), `CommandContext` API |
| **judas-model** | Descriptors, SPIs (`CommandMessageProvider`, `ParameterResolver`, `SuggestionProvider`, …), conditions |
| **judas-engine** | Parsing, routing, binding, completion, execution pipeline |
| **judas-runtime** | `CommandManager`, `CommandManagerOptions`, built-in conditions |
| **judas-bukkit** | Bukkit `CommandManager` + platform command registration |
| **judas-cli** | Reference CLI `CommandManager` + dispatch loop |
| **examples/** | Sample projects for the bundled adapters |

---

## Quick start (Maven)

Add the adapter module you need (for the Bukkit adapter, declare your server API as `provided`):

```xml
<dependency>
    <groupId>dev.revere.judas</groupId>
    <artifactId>judas-bukkit</artifactId>
    <version>x.y.z</version>
</dependency>
```

Replace `x.y.z` with the Judas version you depend on (release, BOM, or local `install`). `groupId` / `artifactId` values match the root project.

For a **non-Bukkit** JVM host, depend on `judas-cli` and/or the framework modules (`judas-runtime`, `judas-api`, …) instead of `judas-bukkit`.

---

## Building locally

From the repository root:

```bash
mvn clean install
```

Installs all modules to your local Maven repository so other projects can depend on `dev.revere.judas` artifacts at the version declared in the root POM.

---

## Core model

1. Subclass **`BaseCommand`** and put handler methods on it (and/or on inner types the parser sees—see examples).
2. Declare a **root** with `@RootCommand` on the class or on one method.
3. Add **subcommands** with `@Subcommand` on methods.
4. Register the holder with **`CommandManager.register(BaseCommand)`**, attach extras with **`registerSub(...)`**, or register a built `CommandDescriptor` manually (see [Registration](#registration)).

At runtime, the platform adapter builds a `CommandContext` (sender + raw args) and calls **`dispatch(descriptor, context)`**.

---

## Annotations (examples)

Each annotation lives under `dev.revere.judas.api.annotation`. Snippets are illustrative; combine them on real handler methods as needed.

### `@RootCommand`

Root name(s), visibility, and optional generated `help` subcommand.

**Targets:** `TYPE`, `METHOD`

```java
@RootCommand(names = {"arena", "a"}, generateHelp = true, hidden = false)
public final class ArenaCommand extends BaseCommand { }
```

Or on a single default handler method when the class has no class-level root:

```java
@RootCommand(names = {"ping"}, generateHelp = true)
public void onPing(@Sender Player sender) { }
```

### `@Subcommand`

Declares a method as a subcommand under a root.

**Target:** `METHOD`

```java
@Subcommand(names = {"create", "c"}, parent = "arena", hidden = false)
public void create(@Sender Player sender) { }
```

`parent` ties the method to a root when the holder exposes multiple roots; otherwise it can be omitted for implicit single-root wiring.

### `@Description` / `@Permission`

Human text and permission string for roots or handlers (supported on class and method where the parser reads them).

```java
@Description("Opens the reporting menu.")
@Permission("myplugin.report.use")
@Subcommand(names = {"open"})
public void open(@Sender Player sender) { }
```

### `@Arg`

Logical **argument** name for binding, help, and derived default switch names (`--arg`).

**Target:** `PARAMETER`

```java
public void give(@Sender Player sender, @Arg("kit") Kit kit) { }
```

### `@Switch`

Valued **CLI switches** (`--times`, `-t`) for the same parameter; optional positional binding via `positional()` (default `true`).

**Target:** `PARAMETER`

```java
public void foo(@Arg("count") @Switch(names = {"--count", "-n"}) int count) { }
```

### `@Flag`

Boolean **presence** switches only (`boolean` / `Boolean`).

**Target:** `PARAMETER`

```java
public void foo(@Arg("quiet") @Flag(names = {"-q", "--quiet"}) boolean quiet) { }
```

`@Switch` and `@Flag` must not appear on the **same** parameter; different parameters on the same method may use one each.

### `@Optional`

Marks a parameter optional for binding when no value is supplied.

**Target:** `PARAMETER`

```java
public void ping(@Sender Player sender, @Arg("target") @Optional Player target) { }
```

### `@DefaultValue`

Default **string** token when an optional parameter is omitted (resolved like a normal argument).

**Target:** `PARAMETER`

```java
public void demo(@Arg("mode") @Optional @DefaultValue("survival") String mode) { }
```

### `@ConsumeRemaining`

Greedy consumption of all remaining tokens into one parameter (typically `String`).

**Target:** `PARAMETER`

```java
public void say(@Arg("message") @ConsumeRemaining String message) { }
```

### `@Sender`

Marks the platform sender parameter (e.g. `Player`). Incompatible with `@ConsumeRemaining` / `@Suggestions` per consistency rules.

**Target:** `PARAMETER`

```java
public void go(@Sender Player player, @Arg("world") String worldName) { }
```

### `@Suggestions`

Tab completion: `SuggestionProvider` class and/or literal list.

**Target:** `PARAMETER`

```java
// Class must implement SuggestionProvider (see Registration), usually with a no-arg ctor unless pre-registered.
@Arg("arena") @Suggestions(MyArenaIds.class) String arenaId;

@Arg("mode") @Suggestions(literals = {"easy", "normal", "hard"}) String mode;
```

### `@Conditions`

Named condition expressions evaluated before execution.

**Targets:** `TYPE`, `METHOD`, `PARAMETER`

```java
@Conditions({"player-only"})
@Subcommand(names = {"vip"})
public void vip(@Sender Player sender) { }

public void warn(@Arg("reason") @Conditions({"argument-not-empty"}) String reason) { }
```

Register keys with `CommandManager.registerCondition` (see [Conditions](#conditions)). The runtime also registers built-in validation handlers: `range`, `min`, `max`, `length`, `regex` (driven by the annotations below).

### `@Range`, `@Min`, `@Max`

Numeric validation (implemented via built-in conditions).

**Target:** `PARAMETER`

```java
@Arg("amount") @Range(min = 1, max = 64) int amount;
@Arg("x") @Min(0) @Max(100) int x;
```

### `@Length`

Length of `CharSequence`, `Collection`, or array.

**Target:** `PARAMETER`

```java
@Arg("tag") @Length(min = 3, max = 16) String tag;
```

### `@Regex`

Full-string regex match for text parameters.

**Target:** `PARAMETER`

```java
@Arg("id") @Regex("^[a-z0-9_]+$") String id;
```

### `@Cooldown`

Cooldown for the handler; optional `TimeUnit`, `CooldownScope`, and `key` suffix.

**Targets:** `TYPE`, `METHOD`

```java
import java.util.concurrent.TimeUnit;
import dev.revere.judas.api.annotation.CooldownScope;

@Cooldown(value = 5, unit = TimeUnit.SECONDS, scope = CooldownScope.SENDER, key = "home")
@Subcommand(names = {"home"})
public void home(@Sender Player sender) { }
```

**`CooldownScope`:** `GLOBAL`, `ROOT`, `METHOD`, `SENDER`.

### `@Async`

Runs the handler on the executor configured in `CommandManagerOptions` (return values still pass through response handlers).

**Targets:** `TYPE`, `METHOD`

```java
@Async
@Subcommand(names = {"reload"})
public void reload(@Sender Player sender) {
    // heavy work
}
```

---

## Registration

All examples assume a `CommandManager` instance (e.g. `BukkitCommandManager`).

### Root commands

```java
commandManager.register(new MyRootCommand());
```

Override exposed names (must supply at least one):

```java
commandManager.register(new MyRootCommand(), "alias1", "alias2");
```

Advanced: build or parse a `CommandDescriptor` yourself, then `commandManager.register(descriptor)`.

### Subcommands

Use **`registerSub(...)`** (one overload set; variadic alias lists cover one or many handlers).

After the **root** is registered:

```java
// Every @Subcommand on the holder, all attached under root "arena"
commandManager.registerSub("arena", new ArenaSubcommands());

// Only methods whose @Subcommand(names = ...) includes list, create, ...
commandManager.registerSub("arena", new ArenaSubcommands(), "list", "create");

// Pre-built CommandMethodDescriptor
commandManager.registerSub("arena", parsedMethodDescriptor);
```

If each method’s `@Subcommand(parent = "…")` names an already registered root, you can omit the explicit root:

```java
// All subcommands from the holder (parent comes from each annotation)
commandManager.registerSub(new SubHolder());

// Only handlers whose subcommand aliases include "stats" (or pass "stats", "view", …)
// Strings match @Subcommand(names = ...), not the root name.
commandManager.registerSub(new SubHolder(), "stats");
```

---

## Suggestion providers

`@Suggestions(SomeProvider.class)` expects `SomeProvider` to implement `dev.revere.judas.model.completion.SuggestionProvider` with a **public no-arg constructor**, unless you pre-register an instance:

```java
commandManager.registerSuggestionProvider(MyIds.class, new MyIds(myService));
```

```java
import java.util.Arrays;
import java.util.List;

public final class MyIds implements SuggestionProvider {
    @Override
    public List<String> suggest(CompletionContext context) {
        return Arrays.asList("a", "b"); // filtered by prefix by the engine
    }
}
```

---

## Parameter resolvers

Map custom **parameter types** (e.g. domain objects) from the argument stream and optionally provide tab completion:

```java
// ArgumentTokenReader: dev.revere.judas.model.resolver.ArgumentTokenReader
commandManager.registerResolver(MyType.class, new ParameterResolver<MyType>() {
    @Override
    public MyType resolve(ParameterResolveContext context) {
        String token = ArgumentTokenReader.requireNext(context);
        return resolveFromToken(token); // throw if invalid
    }

    @Override
    public List<String> complete(CompletionContext ctx) {
        return java.util.Collections.emptyList();
    }
});
```

Typed parameters resolved this way often **do not need** `@Suggestions` if `complete(...)` is implemented.

---

## Conditions

Implement `dev.revere.judas.model.condition.CommandCondition` and register a **key** matching `@Conditions` entries:

```java
commandManager.registerCondition("player-only", ctx -> {
    if (!(ctx.getCommandContext().getSender() instanceof Player)) {
        throw new CommandConditionException("Only players can use this.");
    }
});

commandManager.registerCondition("argument-not-empty", ctx -> {
    Object v = ctx.getParameterValue();
    if (!(v instanceof String) || ((String) v).trim().isEmpty()) {
        throw new CommandConditionException("Value must not be empty.");
    }
});
```

`ConditionContext` exposes the `CommandContext`, root / method descriptors, optional `ParameterDescriptor`, bound `parameterValue`, and the raw `expression` string.

On startup, `CommandManager` also calls **`BuiltinCommandConditions.registerAll`**, wiring: `range`, `min`, `max`, `length`, `regex` (from `@Range` / `@Min` / `@Max` / `@Length` / `@Regex` metadata).

To supply a **custom registry** while still using the manager’s constructor setup, use `CommandManagerOptions.builder().conditionRegistry(registry).build()`.

---

## Overriding messages

Framework-facing strings (unknown command, permissions, binding errors, cooldowns, etc.) come from **`CommandMessageProvider`**. Supply your implementation via options:

```java
CommandManagerOptions options = CommandManagerOptions.builder()
        .messageProvider(new CommandMessageProvider() {
            @Override
            public String unknownRootCommand(String rootToken) {
                return "Unknown: " + rootToken;
            }
            // … implement remaining methods …
        })
        .build();
BukkitCommandManager manager = new BukkitCommandManager(plugin, options);
```

See `dev.revere.judas.model.spi.CommandMessageProvider` for the full method list (`noPermissionForRoot`, `bindingError`, `conditionError`, `cooldownActive`, `executionError`, …). The Bukkit example plugin includes a colored implementation: `BukkitExampleMessageProvider`.

Related SPIs on the same builder:

- **`helpFormatter`** (`CommandHelpFormatter`) — usage / help text layout  
- **`helpSubcommandName`** — generated help subcommand label (default `"help"`)  
- **`showUsageAfterBindingError`** — append usage hints after parse/bind failures  
- **`logger`**, **`cooldownService`**, **`asyncExecutor`**, **`addResponseHandler`**, **`addMiddleware`**

---

## Platforms

- **Bukkit:** `dev.revere.judas.bukkit.BukkitCommandManager` — registers into the server command map. Construct with `Plugin` + optional `CommandManagerOptions`.
- **CLI:** `dev.revere.judas.cli.CliCommandManager` — `execute(CliCommandSender, String line)` dispatches against registered roots (optional leading `/` stripped).

Example projects: `examples/judas-example-bukkit`, `examples/judas-example-cli`.

### Tab completion

Tab completion goes through `CommandManager.complete(descriptor, CompletionAdapter, args)`. The engine’s `CommandCompletionService` uses **`CompletionAdapter.hasPermission`** the same way execution uses permissions: if the sender lacks the root `@Permission`, they get **no** candidates for that command. Subcommand aliases, switches/flags, and parameter suggestions are only offered for handlers and parameters the sender is allowed to use (`@Permission` on the subcommand or default handler). Your adapter should delegate to the host’s permission model (e.g. Bukkit’s `CommandSender.hasPermission`).

Which commands appear in the client’s **global** command list is still partly controlled by the host (e.g. Bukkit `plugin.yml` and server config); Judas ensures **its** completion results do not leak disallowed routes inside a registered root.

---

## License

MIT License

Copyright (c) 2026 Revere Group

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.