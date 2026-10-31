package com.forjasuprema.treatment;

import java.util.Optional;

/** Tratamentos termicos e superficiais. O id sem namespace casa com os ids de {@code treatments} do metal. Classe pura. */
public enum TreatmentKind {
    /** Tempera: imersao rapida da peca quente em agua; endurece e fragiliza. */
    QUENCH("quench"),
    /** Recozimento: manutencao em temperatura; remove dureza e fragilidade. */
    ANNEALING("annealing"),
    /** Revenimento: reaquecimento suave depois da tempera; reduz a fragilidade. */
    TEMPERING("tempering"),
    /** Cementacao (case hardening): carbono penetra na superficie de metais de baixa temperabilidade. */
    CARBURIZING("carburizing"),
    /** Endurecimento superficial: aquecimento da superficie com chama forte. */
    SURFACE_HARDENING("surface_hardening");

    private final String id;

    TreatmentKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Id completo, como escrito nos JSONs de metal. */
    public String fullId() {
        return "forjasuprema:" + id;
    }

    public static Optional<TreatmentKind> fromId(String id) {
        for (TreatmentKind kind : values()) {
            if (kind.id.equals(id) || kind.fullId().equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /** Tratamentos feitos no forno (com tempo de permanencia); a tempera e feita no tanque. */
    public boolean usesFurnace() {
        return this != QUENCH;
    }
}
