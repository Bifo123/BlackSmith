package com.forjasuprema.thermal;

/**
 * Fonte de calor que pode receber um impulso temporario (foles). Mantem o minigame de foles
 * desacoplado da implementacao da forja.
 */
public interface Boostable {
    /** Se a fonte esta acesa (foles so funcionam em chama acesa). */
    boolean isLit();

    /**
     * Acelera o aquecimento por um tempo. Nao eleva o teto de temperatura nem o de chama, so a velocidade.
     *
     * @param rateFactor multiplicador da taxa de aquecimento (1.0 = sem efeito)
     * @param ticks      duracao do impulso
     */
    void boost(double rateFactor, int ticks);
}
