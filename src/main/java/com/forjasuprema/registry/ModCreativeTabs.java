package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForjaSuprema.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ForjaSuprema.MOD_ID + ".main"))
                    .icon(() -> ModItems.SMITH_HAMMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        ModItems.STATION_ITEMS.forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.SMITH_HAMMER.get());
                        output.accept(ModItems.BELLOWS.get());
                        output.accept(ModItems.TIN_ORE.get());
                        output.accept(ModItems.RAW_TIN.get());
                        output.accept(ModItems.TIN_INGOT.get());
                        output.accept(ModItems.BRONZE_INGOT.get());
                        output.accept(ModItems.STEEL_INGOT.get());
                        output.accept(ModItems.SLAG.get());
                        // Pecas forjadas: uma de cada formato para cada metal registrado (so existe com mundo carregado).
                        parameters.holders().lookup(ModRegistries.METAL).ifPresent(metals ->
                                metals.listElementIds().forEach(key -> {
                                    for (var part : java.util.List.of(ModItems.METAL_PLATE, ModItems.METAL_ROD, ModItems.METAL_NAILS)) {
                                        ItemStack stack = new ItemStack(part.get());
                                        stack.set(ModDataComponents.METAL_PART.get(), key.location());
                                        output.accept(stack);
                                    }
                                }));
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
