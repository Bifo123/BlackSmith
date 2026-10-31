package com.forjasuprema.minigame;

/**
 * Como um processo de fabricacao esta sendo executado.
 *
 * <p>Regra do documento mestre (6.2): em {@link #AUTOMATED} (maquinas do Create, Fase 10) os minigames ficam
 * <b>completamente desativados</b>: nenhuma sessao e criada e nenhuma interface e aberta. As
 * sessoes de {@code MinigameManager} so existem para {@link #MANUAL}; processos automatizados usam o
 * avaliador proprio da Fase 10, sem passar pelo gerenciador de minigames.
 */
public enum ProcessingMode {
    MANUAL,
    AUTOMATED;

    /** Minigames so podem ocorrer em processos manuais. */
    public boolean allowsMinigames() {
        return this == MANUAL;
    }
}
