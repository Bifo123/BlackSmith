package com.forjasuprema.minigame;

/** Como uma sessao de minigame terminou. Classe pura. */
public enum MinigameEnd {
    COMPLETED,
    /** O jogador fechou a tela. */
    CANCELLED,
    /** Passou do tempo maximo da sessao. */
    TIMEOUT,
    /** A peca esfriou demais durante o martelamento (resfriamento prematuro). */
    COOLED,
    /** A peca foi removida ou trocada da estacao. */
    PIECE_CHANGED,
    TOO_FAR,
    /** O jogador trocou a ferramenta da mao. */
    TOOL_CHANGED,
    /** Logout, morte, troca de dimensao ou bloco quebrado. */
    INTERRUPTED;

    public static MinigameEnd fromOrdinal(int ordinal) {
        MinigameEnd[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
