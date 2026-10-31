package com.forjasuprema.minigame;

/** Tipos de minigame. Classe pura. */
public enum MinigameKind {
    /** Martelamento na bigorna: golpes com direcao, modelam a peca. */
    HAMMERING(true),
    /** Foles na forja: sopro ritmado que acelera o aquecimento. */
    BELLOWS(false);

    private final boolean directional;

    MinigameKind(boolean directional) {
        this.directional = directional;
    }

    /** Se cada golpe exige uma direcao especifica. */
    public boolean directional() {
        return directional;
    }

    public static MinigameKind fromOrdinal(int ordinal) {
        MinigameKind[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
