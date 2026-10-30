package com.forjasuprema.command;

import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.metal.alloy.AlloyInput;
import com.forjasuprema.metal.alloy.AlloyRecipe;
import com.forjasuprema.metal.alloy.AlloyService;
import com.forjasuprema.registry.ModRegistries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos de inspecao e depuracao ({@code /forjasuprema ...}, nivel de permissao 2).
 * Servem para validar a Fase 2 antes de existirem estacoes e interface.
 */
public final class ForjaCommands {
    private static final SuggestionProvider<CommandSourceStack> METAL_IDS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    context.getSource().registryAccess().registryOrThrow(ModRegistries.METAL).keySet(), builder);

    private ForjaCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        ArgumentType<ResourceLocation> idArgument = ResourceLocationArgument.id();
        dispatcher.register(Commands.literal("forjasuprema")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("metal")
                        .then(Commands.literal("list").executes(ForjaCommands::listMetals))
                        .then(Commands.literal("held").executes(ForjaCommands::heldMetal))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", idArgument)
                                        .suggests(METAL_IDS)
                                        .executes(ForjaCommands::metalInfo))))
                .then(Commands.literal("alloy").executes(ForjaCommands::alloy)));
    }

    private static int listMetals(CommandContext<CommandSourceStack> context) {
        Registry<Metal> registry = context.getSource().registryAccess().registryOrThrow(ModRegistries.METAL);
        String ids = registry.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.forjasuprema.metal.list", registry.size(), ids), false);
        return registry.size();
    }

    private static int metalInfo(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        Registry<Metal> registry = context.getSource().registryAccess().registryOrThrow(ModRegistries.METAL);
        Metal metal = registry.get(id);
        if (metal == null) {
            context.getSource().sendFailure(Component.translatable("commands.forjasuprema.metal.unknown", id.toString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.forjasuprema.metal.info",
                id.toString(),
                metal.tier(),
                fmt(metal.hardness()),
                fmt(metal.strength()),
                fmt(metal.malleability()),
                fmt(metal.hardenability()),
                metal.meltTemperature(),
                metal.workTempMin(),
                metal.workTempMax()), false);
        return 1;
    }

    private static int heldMetal(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        Optional<Holder.Reference<Metal>> metal = MetalLookup.find(player.registryAccess(), stack);
        if (metal.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.forjasuprema.metal.held.none"));
            return 0;
        }
        String id = metal.get().key().location().toString();
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands.forjasuprema.metal.held", stack.getHoverName(), id), false);
        return 1;
    }

    private static int alloy(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<ItemStack> stacks = player.getInventory().items.stream().filter(stack -> !stack.isEmpty()).toList();
        Optional<RecipeHolder<AlloyRecipe>> found = AlloyService.find(player.level(), new AlloyInput(stacks));
        if (found.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.forjasuprema.alloy.none"));
            return 0;
        }
        AlloyRecipe recipe = found.get().value();
        int batches = AlloyService.craftFromInventory(player, recipe);
        if (batches <= 0) {
            context.getSource().sendFailure(Component.translatable("commands.forjasuprema.alloy.none"));
            return 0;
        }
        ItemStack result = recipe.result();
        context.getSource().sendSuccess(() -> Component.translatable("commands.forjasuprema.alloy.ok",
                result.getHoverName(), result.getCount() * batches, batches), false);
        return batches;
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
