package com.forjasuprema.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuracao comum (servidor e cliente leem o mesmo arquivo). Novas opcoes entram junto de cada fase. */
public final class ForjaConfig {
    public static final ModConfigSpec SPEC;

    /** 0 = niveis ilimitados. */
    public static final ModConfigSpec.IntValue MAX_SMITH_LEVEL;
    public static final ModConfigSpec.BooleanValue ENABLE_CREATE_COMPAT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Progressao do ferreiro").push("progression");
        MAX_SMITH_LEVEL = builder
                .comment("Nivel maximo do ferreiro. 0 = ilimitado.")
                .defineInRange("maxSmithLevel", 0, 0, 100_000);
        builder.pop();

        builder.comment("Compatibilidade com outros mods").push("compat");
        ENABLE_CREATE_COMPAT = builder
                .comment("Ativa a integracao com Create quando ele estiver instalado.")
                .define("enableCreateCompat", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ForjaConfig() {
    }
}
