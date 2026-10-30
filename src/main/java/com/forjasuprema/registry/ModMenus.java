package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.station.AlloyStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ForjaSuprema.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlloyStationMenu>> ALLOY_STATION =
            MENUS.register("alloy_station", () -> IMenuTypeExtension.create(AlloyStationMenu::new));

    private ModMenus() {
    }
}
