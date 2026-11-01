package com.forjasuprema.finish;

import com.forjasuprema.minigame.MinigameKind;
import net.minecraft.world.item.Item;

/** Ferramenta de acabamento (pano, lima, cinzel): so funciona na estacao do seu tipo. */
public class FinishToolItem extends Item {
    private final MinigameKind kind;
    private final float quality;

    public FinishToolItem(Properties properties, MinigameKind kind, float quality) {
        super(properties);
        this.kind = kind;
        this.quality = quality;
    }

    public MinigameKind kind() {
        return kind;
    }

    /** Qualidade da ferramenta (0 a 1): reduz a dificuldade do minigame. */
    public float quality() {
        return quality;
    }
}
