# Forja Suprema — Arquitetura Técnica

Alvo: **Minecraft 1.21.1 · NeoForge 21.1.x · Java 21**. Create (opcional) na linha 6.x para 1.21.1.

Este documento responde ao item 20 do documento mestre: arquitetura, dependências entre sistemas, conflitos, cálculo de qualidade, processos térmicos, integração com Create e estrutura de pastas/classes. Cada fase de implementação deve manter este documento atualizado.

---

## 1. Princípios

1. **Servidor é a autoridade.** O cliente só apresenta UI e envia *intenções* (golpe na posição X no tick T). Temperatura, validação de tratamentos, qualidade e XP são calculados e persistidos apenas no servidor.
2. **Dados, não código.** Metais, ligas, combustíveis, tratamentos e receitas são definidos por datapack (JSON). Outros mods e modpacks adicionam materiais sem tocar no código (seção 8).
3. **Um único pipeline para manual e automático.** O mesmo validador térmico e o mesmo cálculo de qualidade servem aos dois modos. Só muda *quem avalia cada etapa*: minigame (manual) ou configuração de máquina (Create).
4. **Create é plugin.** Nenhuma classe do pacote principal importa `com.simibubi.create`. Toda referência fica em `compat.create`, carregada só se o mod estiver presente.
5. **Sem tick por item.** Temperatura de peças é calculada de forma preguiçosa (seção 4.3).

## 2. Mapa de pacotes

```
com.forjasuprema
├── ForjaSuprema.java            # entrada do mod, liga registros e compat
├── config/                      # ForjaConfig (ModConfigSpec)
├── registry/                    # ModItems, ModBlocks, ModCreativeTabs, ModRegistries, ModRecipes,
│                                #   (futuro) ModDataComponents, ModAttachments, ModMenus, ModBlockEntities
├── command/                     # /forjasuprema: inspeção e depuração (permissão 2)
├── metal/                       # Metal (record+codec), MetalLookup                      (Fase 2 ✔)
│   └── alloy/                   # AlloyRecipe, AlloyInput, AlloyService                   (Fase 2 ✔)
├── thermal/                     # lógica pura: ThermalMath, PieceThermal, FlameLevel, ForgeTier,
│                                #   HeatRequirement (sem Minecraft, testada com JUnit);
│                                #   com Minecraft: Fuel/FuelLookup (datapack), HeatData (componente
│                                #   do item), HeatSource (interface), HeatEvents            (Fase 4 ✔)
├── station/                     # WorkpieceStationBlock/BlockEntity (peça sobre a estação),
│                                #   AlloyStationBlock/BlockEntity/Menu                     (Fase 3 ✔)
│                                #   ForgeBlock/ForgeBlockEntity (chama, temperatura, peça)   (Fase 4 ✔)
├── minigame/                    # sessões, regras de pontuação, validação server-side    (Fase 5)
├── treatment/                   # Treatment, TreatmentValidator, resultados              (Fase 6)
├── craft/                       # acabamento: polimento, afiação, entalhes, runas        (Fase 7)
├── quality/                     # QualityTier, StageScore, QualityCalculator, perks      (Fase 8)
├── progression/                 # SmithData (attachment), SmithRank, Passive, XP         (Fase 9)
├── network/                     # payloads (cliente -> servidor e servidor -> cliente)
├── client/                      # telas, HUD, renderers (nada daqui é carregado no servidor)
├── compat/
│   ├── CompatManager.java       # detecção de mods por ModList
│   └── create/                  # ÚNICO lugar que referencia Create                      (Fase 10)
└── util/
```

Regra de dependência (setas = "pode importar"):

```
client ──► network ──► minigame ──► treatment ──► quality ──► metal
   │                       │            │            │          ▲
   │                       └────────────┴──► thermal ┘          │
   └──────────────────────────────────────────────────────── station ──► registry
progression ──► quality, metal          compat.* ──► (qualquer pacote acima, nunca o inverso)
```

`metal`, `thermal` e `quality` são lógica pura (sem classes de cliente, testáveis em JUnit sem iniciar o jogo).

## 3. Dados persistidos

| Dado | Mecanismo | Observação |
|---|---|---|
| Nível, XP, passivas do jogador | **Data attachment** do jogador com codec, `copyOnDeath` | sincroniza com o cliente dono para UI |
| Qualidade, notas por etapa, atributos extras | **Data components** no ItemStack | codec + stream codec; imutáveis, validados no servidor |
| Temperatura da peça | Data component `heat` = (temperatura, gameTime) | ver 4.3 |
| Estado da forja (combustível, chama, temperatura) | Block entity (NBT) | ticking só enquanto a chama estiver acesa |
| Metais, combustíveis, tratamentos | Registros de datapack | recarregáveis com `/reload` |

Prevenção de duplicação: peça em processo é *movida* (não copiada) para o block entity/sessão; a sessão guarda o ID e é invalidada se o jogador sair, morrer ou o bloco for quebrado (o item volta ao inventário/dropa).

## 4. Processos térmicos

> **Implementado na Fase 4.** As fórmulas abaixo estão em `thermal/ThermalMath` e `thermal/PieceThermal` (taxas por tick, aproximação exponencial `T += (alvo - T) * (1 - e^-taxa)`). Valores reais (tiers, combustíveis, taxas) estão no README e nos JSONs; este capítulo descreve o modelo. A fonte de calor é abstraída por `HeatSource`, então a Estação de Ligas, os tratamentos (Fase 6) e as máquinas do Create (Fase 10) consultam a mesma interface. A progressão fica fixada em teste: bronze (chama 3, 1100 °C) na forja de Ferreiro; aço (chama 4, 1500 °C) só na forja de Mestre.

### 4.1 Níveis de chama

`FlameLevel`: `OFF(0) · WEAK(1) · MODERATE(2) · STRONG(3) · INTENSE(4)`.

O nível é derivado de: combustível queimando (intensidade base), quantidade/estado do combustível, **tier da estrutura** (Aprendiz → Lendária adiciona bônus e teto) e ar/foles (opcional, futuro). `flame = min(fuel.intensity + structure.bonus, structure.maxFlame)`.

### 4.2 Temperatura da forja

`forgeTemp` aproxima-se de `targetTemp = min(fuel.maxTemp, structure.maxTemp) * efficiency(flame)` com aproximação exponencial por tick (só no block entity). Combustível acabando reduz `flame` gradualmente, então a forja esfria de forma legível.

### 4.3 Temperatura das peças (cálculo preguiçoso)

Item guarda `(T0, t0)`. A temperatura atual é calculada sob demanda:

- Em contato com a forja: `T(t) = Tf - (Tf - T0) * e^(-kHeat * m * Δt)`
- Fora da forja: `T(t) = Ta + (T0 - Ta) * e^(-kCool * Δt)`

`m` depende da maleabilidade/massa do metal. Nenhum item é percorrido a cada tick; só se recalcula quando alguém lê (tooltip, bigorna, tanque). Resolve multiplayer (servidor calcula, cliente recebe valor para exibir) e desempenho.

### 4.4 Janelas e superaquecimento

Cada metal define `workTemp [min,max]` e `meltTemp`. Abaixo de `min`: martelar falha/penaliza. Acima de `max`: penalidade de dureza (queima); acima de `meltTemp`: peça vira "escória" ou volta a lingote, conforme material. Nunca destruição silenciosa.

### 4.5 Tratamentos e a regra da têmpera

Cada `Treatment` (datapack) declara: `minFlame`, janela de temperatura, tempo de manutenção, meio de resfriamento e taxa de resfriamento.

```
TreatmentValidator.validate(piece, forgeState, treatment) -> Outcome
  1. forgeState.flame < treatment.minFlame      -> FAILED(INSUFFICIENT_FLAME)
  2. piece.temp fora da janela no início        -> FAILED(WRONG_TEMPERATURE)
  3. meio de resfriamento incompatível          -> FAILED(WRONG_MEDIUM)
  4. senão                                      -> VALID(precisão = f(taxa de resfriamento, tempo na janela))
```

**Regra fundamental (4.4 do mestre):** as verificações 1–3 rodam *antes* e independem do minigame. Um minigame perfeito não converte um `FAILED` em sucesso. Falha aplica a classificação "tratamento inadequado" (nota térmica reduzida e marca no item), não destrói a peça; o material define se pode repetir.

O mesmo validador vale para recozimento, revenimento, cementação e endurecimento superficial (cada um com sua tabela) e para máquinas.

## 5. Cálculo de qualidade

Cada etapa produz uma nota `s ∈ [0,1]`: **material**, **modelagem**, **térmica**, **acabamento** (e, opcional, **ferramentas**). A receita define pesos `w` (somam 1) e quais etapas existem — etapas ausentes são ignoradas e os pesos renormalizados.

```
base   = Σ(w_i · s_i)
teto   = min_i(s_i) + 0.35          # uma etapa ruim limita o resultado
Q      = min(base, teto)            # nenhum minigame sozinho carrega a peça
```

Faixas de `Q` → classificação:

| Q | Classe |
|---|---|
| < 0.20 | Defeituoso |
| 0.20 – 0.40 | Rudimentar |
| 0.40 – 0.60 | Comum |
| 0.60 – 0.78 | Bom |
| 0.78 – 0.90 | Excelente |
| 0.90 – 0.97 | Perfeito |
| ≥ 0.97 + condições | Lendário |

**Lendário** exige, além de `Q ≥ 0.97`: todas as etapas `≥ 0.90`, material de tier raro ou superior, receita marcada como `legendary_capable` e forja com tier mínimo. **Sem aleatoriedade.**

**Regras 1 e 2 do mestre (habilidade, não sorte):** nenhum ponto da fórmula usa RNG. Nível e passivas alteram a *tolerância* dos minigames (janela de acerto) e bônus pontuais declarados, nunca a nota diretamente. Atributos extras de itens Perfeito/Lendário são sorteados com semente determinística (hash de receita + notas por etapa + material), então o mesmo desempenho produz o mesmo resultado e não dá para "rerrolar" o item.

## 6. Minigames e segurança em multiplayer

- Servidor abre uma `ForgeSession` (jogador, peça, receita, estação, tick inicial) e envia ao cliente os parâmetros (padrão de golpes, janelas).
- Cliente envia eventos `(tipoDeGolpe, tickRelativo)`; o servidor **recalcula** o score comparando com o padrão e com a temperatura real da peça naquele tick.
- Limites de taxa, sessão única por jogador, timeout, cancelamento seguro (peça devolvida).
- Se o cliente mentir, só perde pontuação: nada que ele envia é aceito como resultado.

## 7. Integração com Create (isolada)

```
ProcessingContext { mode: MANUAL | AUTOMATED, ... }
StageEvaluator    { evaluate(stage, context) -> StageScore }
  ├─ ManualEvaluator     (usa resultados de minigame da sessão)         # núcleo
  └─ MachineEvaluator    (usa config da máquina: precisão, tier)       # compat.create
```

- Modo `AUTOMATED` **nunca abre** menus ou sessões de minigame (regra 6.2).
- O `TreatmentValidator` é o mesmo: máquina sem calor suficiente falha a têmpera (6.3).
- A nota de etapa automatizada é limitada pelo tier da máquina e pela configuração, com teto configurável (padrão 0.85), para que a automação seja eficiente mas o topo da qualidade continue exigindo habilidade manual. Ajustável em config.
- Carregamento: `CompatManager` checa `ModList.get().isLoaded("create")` e só então referencia `compat.create.CreateCompat`. O `neoforge.mods.toml` declara Create como dependência `optional`. Sem Create, nenhuma classe dele é resolvida.

## 8. Materiais e compatibilidade com outros mods

- Registro de datapack `forjasuprema:metal` (id, tag de item/lingote, dureza, resistência, maleabilidade, fusão, janela de forja, compatibilidade de tratamento, tier de qualidade, propriedades especiais).
- Identificação por **tags** (`c:ingots/*`, `c:ores/*`) com valores *padrão* seguros para metais desconhecidos, de modo que um metal de outro mod funcione mesmo sem JSON dedicado.
- Receitas por `RecipeType` próprio e serializers, todas configuráveis por datapack.

## 9. Conflitos entre mecânicas e decisões

| Conflito | Decisão |
|---|---|
| Automação mais barata que o manual desequilibra a economia | Teto de qualidade automatizada + custo/tier de máquina; tudo configurável |
| Passivas de precisão não fazem sentido na automação | Passivas "de mão" só afetam sessões manuais; passivas de material/térmica podem valer em ambos |
| Temperatura "ao vivo" vs. sincronização | Cálculo preguiçoso: nada é sincronizado por tick, apenas parâmetros |
| Reparo/bigorna vanilla vs. qualidade do item | Componentes de qualidade preservados no reparo; renomear não apaga; combinar itens recalcula por regra explícita |
| Atributos extras vs. balanceamento de outros mods | Modificadores de atributo com IDs próprios, valores com teto em config |
| Cliente confiável em minigame | Resultado sempre recomputado no servidor |
| Sem Create instalado | Zero imports de Create fora de `compat.create` |
| Falha de têmpera destrói progresso do jogador | Falha rebaixa a nota térmica e marca o item; destruição só em casos extremos do material |

## 10. Plano de fases (resumo)

1. **Arquitetura e esqueleto** — projeto, registros, config, compat isolado, este documento. ✔ nesta entrega
2. Materiais e metalurgia básica
3. Blocos e estações
4. Sistema térmico e combustíveis
5. Minigames
6. Técnicas metalúrgicas
7. Artesanato e acabamento
8. Qualidade e equipamentos
9. Progressão
10. Integração com Create
11. Compatibilidade externa
12. Testes e otimização

Cada fase termina com: arquivos criados, funções, como testar e correção de erros antes de avançar.
