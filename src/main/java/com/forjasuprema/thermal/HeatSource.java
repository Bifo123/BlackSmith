package com.forjasuprema.thermal;

/**
 * Qualquer coisa que forneca calor a um processo (forja, e no futuro maquinas do Create).
 * A estacao de ligas e os tratamentos da Fase 6 consultam esta interface, nunca a forja diretamente.
 */
public interface HeatSource {
    /** Intensidade da chama, de 0 (apagada) a 4 (intensa). */
    int flameLevel();

    /** Temperatura atual da fonte, em graus Celsius. */
    double temperature();
}
