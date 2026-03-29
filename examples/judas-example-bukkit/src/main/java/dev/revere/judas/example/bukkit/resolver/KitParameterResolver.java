package dev.revere.judas.example.bukkit.resolver;

import dev.revere.judas.example.bukkit.model.Kit;
import dev.revere.judas.example.bukkit.service.KitService;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolveContext;
import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.List;

/**
 * Resolves {@link Kit} parameters from kit ids.
 *
 * <p>Important distinction:
 * this resolver owns both parsing (String -> Kit) and tab-completion for Kit-typed parameters.
 * That means command methods with {@code Kit} parameters do not need {@code @Suggestions}.
 */
public final class KitParameterResolver implements ParameterResolver<Kit> {
    private final KitService kitService;

    public KitParameterResolver(KitService kitService) {
        this.kitService = kitService;
    }

    @Override
    public Kit resolve(ParameterResolveContext context) {
        String token = ArgumentTokenReader.requireNext(context);
        Kit kit = this.kitService.findById(token);
        if (kit == null) {
            throw new IllegalArgumentException("Kit not found: " + token);
        }
        return kit;
    }

    @Override
    public List<String> complete(CompletionContext context) {
        return this.kitService.suggestIds(context.getPartial());
    }
}
