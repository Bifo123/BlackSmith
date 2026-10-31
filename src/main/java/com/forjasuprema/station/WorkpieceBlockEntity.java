package com.forjasuprema.station;

import com.forjasuprema.minigame.PieceHolder;
import com.forjasuprema.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Guarda a peca que esta sobre uma estacao (forja, bigorna, bancada...). As fases seguintes
 * (temperatura, minigames, tratamentos) operam sobre a peca guardada aqui.
 */
public class WorkpieceBlockEntity extends BlockEntity implements PieceHolder {
    private static final String PIECE_KEY = "Piece";

    private ItemStack piece = ItemStack.EMPTY;

    public WorkpieceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.WORKPIECE.get(), pos, state);
    }

    /** Usado por estacoes com comportamento proprio (ex.: forja), que registram o proprio tipo. */
    protected WorkpieceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getPiece() {
        return piece;
    }

    public boolean hasPiece() {
        return !piece.isEmpty();
    }

    /** Define a peca e sincroniza com os clientes. Deve ser chamado apenas no servidor. */
    public void setPiece(ItemStack newPiece) {
        this.piece = newPiece;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Remove e devolve a peca. */
    public ItemStack takePiece() {
        ItemStack taken = piece;
        setPiece(ItemStack.EMPTY);
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!piece.isEmpty()) {
            tag.put(PIECE_KEY, piece.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        piece = tag.contains(PIECE_KEY)
                ? ItemStack.parse(registries, tag.getCompound(PIECE_KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
