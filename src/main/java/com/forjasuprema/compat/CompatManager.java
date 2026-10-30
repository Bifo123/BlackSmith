package com.forjasuprema.compat;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.compat.create.CreateCompat;
import com.forjasuprema.config.ForjaConfig;
import net.neoforged.fml.ModList;

/**
 * Detecta mods opcionais e ativa suas integracoes.
 *
 * <p>Regra: classes de outros mods so sao referenciadas dentro de {@code compat.<mod>}, e esses
 * pacotes so sao tocados depois de confirmar via {@link ModList} que o mod esta presente.
 * Falha de integracao nunca derruba o jogo.
 */
public final class CompatManager {
    public static final String CREATE = "create";

    private CompatManager() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static void init() {
        if (isLoaded(CREATE) && ForjaConfig.ENABLE_CREATE_COMPAT.get()) {
            try {
                CreateCompat.init();
            } catch (Throwable t) {
                ForjaSuprema.LOGGER.error("Falha ao ativar a integracao com Create; seguindo sem ela.", t);
            }
        }
    }
}
