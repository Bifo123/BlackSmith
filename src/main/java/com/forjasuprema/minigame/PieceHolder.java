package com.forjasuprema.minigame;

import net.minecraft.world.item.ItemStack;

/** Qualquer estacao que guarda uma peca de trabalho; permite aos minigames ler e trocar a peca sem depender das estacoes. */
public interface PieceHolder {
    ItemStack getPiece();

    /** Define a peca. Chamado apenas no servidor. */
    void setPiece(ItemStack piece);
}
