package com.forjasuprema.craft;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.finish.FinishState;
import com.forjasuprema.finish.RuneItem;
import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.quality.EquipmentStats;
import com.forjasuprema.quality.ExtraAttributes;
import com.forjasuprema.quality.QualityCalculator;
import com.forjasuprema.quality.QualityData;
import com.forjasuprema.quality.QualityTier;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.treatment.TreatmentState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Monta equipamentos a partir das pecas forjadas: calcula a qualidade (regras puras em
 * {@code quality}), os atributos, as runas e grava tudo no item. Sempre no servidor.
 */
public final class AssemblyService {
    private static final List<String> TREATMENT_STAGES = List.of("quench", "tempering", "carburizing", "surface_hardening");

    /** {@code message} nao nulo = falhou e nada foi consumido. */
    public record Outcome(Component message, ItemStack result, QualityCalculator.Result quality) {
        public boolean ok() {
            return message == null;
        }
    }

    private AssemblyService() {
    }

    /** Formato da peca ({@code plate}, {@code rod}, {@code nails}) ou {@code null} se nao for uma peca montavel. */
    public static String shapeOf(ItemStack stack) {
        if (stack.is(ModItems.METAL_PLATE.get())) {
            return "plate";
        }
        if (stack.is(ModItems.METAL_ROD.get())) {
            return "rod";
        }
        if (stack.is(ModItems.METAL_NAILS.get())) {
            return "nails";
        }
        return null;
    }

    public static Outcome assemble(RegistryAccess access, Level level, List<ItemStack> parts,
                                   List<RecipeHolder<AssemblyRecipe>> recipes) {
        if (parts.isEmpty()) {
            return fail("message.forjasuprema.bench.empty");
        }
        ResourceLocation metalId = parts.get(0).get(ModDataComponents.METAL_PART.get());
        if (metalId == null || parts.stream().anyMatch(p -> !metalId.equals(p.get(ModDataComponents.METAL_PART.get())))) {
            return fail("message.forjasuprema.bench.mixed_metals");
        }
        AssemblyInput input = new AssemblyInput(parts);
        Optional<RecipeHolder<AssemblyRecipe>> match = recipes.stream().filter(r -> r.value().matches(input, level)).findFirst();
        if (match.isEmpty()) {
            return fail("message.forjasuprema.bench.no_recipe");
        }
        AssemblyRecipe recipe = match.get().value();
        ResourceLocation itemId = recipe.results().get(metalId);
        Optional<Item> item = itemId == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) {
            return fail("message.forjasuprema.bench.metal_unsuitable");
        }
        Optional<Holder.Reference<Metal>> metalHolder = MetalLookup.find(access, parts.get(0));
        if (metalHolder.isEmpty()) {
            return fail("message.forjasuprema.bench.metal_unsuitable");
        }
        Metal metal = metalHolder.get().value();

        // ---- resumo das pecas
        List<QualityCalculator.PartSummary> summaries = new ArrayList<>();
        double hardnessSum = 0.0;
        double brittleness = 0.0;
        double sharpness = 0.0;
        int forgeTier = Integer.MAX_VALUE;
        List<StageScores> allScores = new ArrayList<>();
        for (ItemStack part : parts) {
            StageScores scores = part.getOrDefault(ModDataComponents.STAGE_SCORES.get(), StageScores.empty());
            allScores.add(scores);
            FinishState finish = part.getOrDefault(ModDataComponents.FINISH.get(), FinishState.NONE);
            TreatmentState treatment = part.getOrDefault(ModDataComponents.TREATMENT.get(), TreatmentState.NONE);
            // Media das etapas de tratamento realizadas (0 se a peca nao recebeu nenhuma).
            double treatmentScore = TREATMENT_STAGES.stream().filter(key -> scores.scores().containsKey(key))
                    .mapToDouble(key -> score(scores, key, 0.0)).average().orElse(0.0);
            OptionalDouble weld = scores.scores().containsKey(StageScores.WELD)
                    ? OptionalDouble.of(score(scores, StageScores.WELD, 0.0)) : OptionalDouble.empty();
            summaries.add(new QualityCalculator.PartSummary(score(scores, StageScores.MODELING, 0.0),
                    score(scores, StageScores.THERMAL, 1.0), weld, treatmentScore, finish.polish(), finish.sharpness()));
            hardnessSum += treatment.hardness();
            brittleness = Math.max(brittleness, treatment.brittleness());
            sharpness = Math.max(sharpness, finish.sharpness());
            forgeTier = Math.min(forgeTier, part.getOrDefault(ModDataComponents.FORGE_TIER.get(), 0));
        }
        boolean treatmentApplicable = metal.treatments().stream()
                .anyMatch(id -> TREATMENT_STAGES.contains(id.getPath()));
        double material = QualityCalculator.materialScore(metal.strength(), metal.hardness());
        Map<String, Double> stages = QualityCalculator.aggregateStages(summaries, material, treatmentApplicable);
        QualityCalculator.Result quality = QualityCalculator.evaluate(stages, recipe.weights(),
                new QualityCalculator.LegendaryContext(recipe.legendaryCapable(), metal.tier(), forgeTier));

        // ---- atributos
        QualityTier tier = quality.tier();
        double totalHardness = Math.min(1.0, metal.hardness() + hardnessSum / parts.size());
        double damage = EquipmentStats.attackDamage(recipe.attackDamage(),
                EquipmentStats.materialFactor(metal.strength(), totalHardness), tier, sharpness);
        int durability = EquipmentStats.durability(recipe.durability(), metal.strength(), tier, brittleness);

        ItemStack stack = new ItemStack(item.get());
        ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder();
        attributes.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "damage"), damage - 1.0,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        attributes.add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "speed"),
                recipe.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        int index = 0;
        long seed = ExtraAttributes.seed(match.get().id().toString(), metalId.toString(), stages);
        for (ExtraAttributes.Bonus bonus : ExtraAttributes.select(seed, tier)) {
            addExtra(attributes, bonus, index++);
        }

        FinishState mergedFinish = mergeFinish(parts);
        for (FinishState.RuneMark mark : mergedFinish.runes()) {
            index = applyRune(access, stack, attributes, mark, index);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes.build());
        stack.set(DataComponents.MAX_DAMAGE, durability);
        stack.set(DataComponents.RARITY, rarityOf(tier));

        stack.set(ModDataComponents.QUALITY.get(), new QualityData(tier.id(), quality.score()));
        stack.set(ModDataComponents.METAL_PART.get(), metalId);
        stack.set(ModDataComponents.FINISH.get(), mergedFinish);
        stack.set(ModDataComponents.TREATMENT.get(), mergeTreatment(parts));
        stack.set(ModDataComponents.STAGE_SCORES.get(), mergeScores(allScores, quality));
        stack.set(ModDataComponents.FORGE_TIER.get(), forgeTier == Integer.MAX_VALUE ? 0 : forgeTier);
        stack.remove(ModDataComponents.HEAT.get());
        return new Outcome(null, stack, quality);
    }

    private static Outcome fail(String key) {
        return new Outcome(Component.translatable(key), ItemStack.EMPTY, null);
    }

    private static double score(StageScores scores, String key, double fallback) {
        Float value = scores.scores().get(key);
        return value == null ? fallback : value;
    }

    private static Rarity rarityOf(QualityTier tier) {
        return switch (tier) {
            case EXCELLENT -> Rarity.UNCOMMON;
            case PERFECT -> Rarity.RARE;
            case LEGENDARY -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    private static void addExtra(ItemAttributeModifiers.Builder builder, ExtraAttributes.Bonus bonus, int index) {
        Holder<Attribute> attribute;
        AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;
        switch (bonus.attribute()) {
            case "attack_speed" -> attribute = Attributes.ATTACK_SPEED;
            case "movement_speed" -> {
                attribute = Attributes.MOVEMENT_SPEED;
                operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            }
            case "max_health" -> attribute = Attributes.MAX_HEALTH;
            case "knockback_resistance" -> attribute = Attributes.KNOCKBACK_RESISTANCE;
            default -> {
                return;
            }
        }
        builder.add(attribute, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "extra_" + index), bonus.amount(), operation),
                EquipmentSlotGroup.MAINHAND);
    }

    /**
     * Efeito de uma runa inscrita (mapeado pelo id do efeito da runa). Efeitos desconhecidos (de datapacks
     * externos) ficam apenas registrados no item.
     */
    private static int applyRune(RegistryAccess access, ItemStack stack, ItemAttributeModifiers.Builder builder,
                                 FinishState.RuneMark mark, int index) {
        ResourceLocation runeId = ResourceLocation.parse(mark.id());
        var runeRegistry = access.registryOrThrow(com.forjasuprema.registry.ModRegistries.RUNE);
        var rune = runeRegistry.getOptional(runeId);
        if (rune.isEmpty()) {
            return index;
        }
        double power = mark.power();
        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "rune_" + index);
        switch (rune.get().effect().getPath()) {
            case "keen" -> builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(modifierId, 2.0 * power,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            case "swift" -> builder.add(Attributes.ATTACK_SPEED, new AttributeModifier(modifierId, 0.5 * power,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            case "warding" -> builder.add(Attributes.ARMOR, new AttributeModifier(modifierId, 3.0 * power,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            case "flame" -> stack.enchant(access.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FIRE_ASPECT), power >= 0.75 ? 2 : 1);
            default -> {
                return index;
            }
        }
        return index + 1;
    }

    /** Polimento medio, melhor fio, e as gravacoes/runas da peca que mais tem. */
    private static FinishState mergeFinish(List<ItemStack> parts) {
        double polish = 0.0;
        double sharp = 0.0;
        FinishState richest = FinishState.NONE;
        for (ItemStack part : parts) {
            FinishState finish = part.getOrDefault(ModDataComponents.FINISH.get(), FinishState.NONE);
            polish += finish.polish();
            sharp = Math.max(sharp, finish.sharpness());
            if (finish.engravings().size() > richest.engravings().size()) {
                richest = finish;
            }
        }
        return new FinishState(polish / parts.size(), sharp, richest.engravings(), richest.runes());
    }

    private static TreatmentState mergeTreatment(List<ItemStack> parts) {
        double hardness = 0.0;
        double brittleness = 0.0;
        List<String> applied = new ArrayList<>();
        for (ItemStack part : parts) {
            TreatmentState state = part.getOrDefault(ModDataComponents.TREATMENT.get(), TreatmentState.NONE);
            hardness += state.hardness();
            brittleness = Math.max(brittleness, state.brittleness());
            state.applied().stream().filter(id -> !applied.contains(id)).forEach(applied::add);
        }
        return new TreatmentState(hardness / parts.size(), brittleness, applied);
    }

    private static StageScores mergeScores(List<StageScores> all, QualityCalculator.Result quality) {
        Map<String, Float> sums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (StageScores scores : all) {
            scores.scores().forEach((key, value) -> {
                sums.merge(key, value, Float::sum);
                counts.merge(key, 1, Integer::sum);
            });
        }
        StageScores merged = StageScores.empty();
        for (Map.Entry<String, Float> entry : sums.entrySet()) {
            merged = merged.with(entry.getKey(), entry.getValue() / counts.get(entry.getKey()));
        }
        // Notas agregadas usadas no calculo (prefixo evita colidir com as notas por etapa individuais).
        for (Map.Entry<String, Double> entry : quality.stages().entrySet()) {
            merged = merged.with("stage_" + entry.getKey(), entry.getValue().floatValue());
        }
        return merged;
    }
}
