package com.forjasuprema.minigame;

/** Direcao de um golpe. {@link #NONE} e usada nos minigames sem direcao (foles). Classe pura. */
public enum StrikeDirection {
    NONE,
    UP,
    DOWN,
    LEFT,
    RIGHT;

    /** Converte um valor vindo da rede; valores invalidos viram {@link #NONE}. */
    public static StrikeDirection fromOrdinal(int ordinal) {
        StrikeDirection[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
