# Forja Suprema (blacksmith)

Mod de **ferraria, metalurgia e artesanato** para Minecraft **1.21.1 (NeoForge)**.
O jogador começa como aprendiz e evolui até forjador lendário: aquece, martela, trata termicamente, acaba e personaliza equipamentos. Integração opcional com **Create** para automação (sem minigames).

## Estado do projeto

| Fase | Descrição | Status |
|---|---|---|
| 1 | Arquitetura e esqueleto do projeto | ✔ entregue |
| 2 | Materiais e metalurgia básica | ✔ entregue |
| 3 | Blocos e estações | ✔ entregue (comportamento de cada estação vem nas fases seguintes) |
| 4 | Temperatura e intensidade da chama | ✔ entregue |
| 5 | Minigames | ✔ entregue |
| 6 | Técnicas metalúrgicas | ✔ entregue |
| 7 | Artesanato e acabamento | pendente |
| 8 | Qualidade e equipamentos | pendente |
| 9 | Progressão | pendente |
| 10 | Integração com Create | pendente |
| 11 | Compatibilidade externa | pendente |
| 12 | Testes e otimização | pendente |

Arquitetura completa: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## O que existe hoje (Fase 1)

- Projeto NeoForge 1.21.1 (ModDevGradle), Java 21.
- Entrada do mod, registros de itens/blocos/aba criativa, configuração (`ForjaConfig`).
- Item de teste: **Martelo de Ferreiro** (textura provisória do ferro).
- `CompatManager`: detecção de Create isolada; o mod inicia normalmente sem ele.
- Estrutura de pacotes preparada para as próximas fases.
- CI no GitHub Actions que compila o projeto a cada push.

## O que existe hoje (Fase 2)

- **Metais por datapack** (`forjasuprema:metal`, em `data/<ns>/forjasuprema/metal/*.json`): dureza, resistência, maleabilidade, endurecibilidade, temperatura de fusão, janela de forjamento, tier, tratamentos compatíveis e propriedades especiais. Incluídos: ferro, cobre, ouro, estanho, bronze e aço. Outros mods/modpacks adicionam metais apenas com um JSON.
- **Estanho**: minério (geração no Overworld, y 0–64), estanho bruto e lingote, fundição em fornalha/alto-forno.
- **Ligas** como tipo de receita próprio (`forjasuprema:alloying`): bronze (3 cobre + 1 estanho → 4 bronze) e aço (2 ferro + 1 carvão → 2 aço). As receitas já carregam `min_flame` e `min_temperature`, que serão exigidos a partir da Fase 4.
- **Comandos de depuração** (permissão 2): `/forjasuprema metal list | info <id> | held` e `/forjasuprema alloy`. O comando `alloy` ignora chama e temperatura até a Fase 4; a estação de ligas (Fase 3) e as máquinas do Create (Fase 10) reutilizam a mesma lógica (`AlloyService`).
- Texturas **provisórias** geradas por `tools/gen_textures.py`.

### Testar a Fase 2

1. `gradle runClient`, crie um mundo criativo e abra a aba **Forja Suprema**.
2. `/forjasuprema metal list` deve listar 6 metais; `/forjasuprema metal info forjasuprema:steel` mostra as propriedades.
3. Segure um lingote de ferro e rode `/forjasuprema metal held` (deve indicar `forjasuprema:iron`).
4. Pegue 3 lingotes de cobre e 1 de estanho e rode `/forjasuprema alloy`: devem virar 4 lingotes de bronze.
5. Em mundo novo, procure minério de estanho entre y 0 e 64 e funda-o em uma fornalha.

## O que existe hoje (Fase 3)

As 10 estações existem como blocos craftáveis, com loot, ferramenta correta de mineração e texturas provisórias (`tools/gen_station_assets.py`):

| Estação | Estado atual |
|---|---|
| Estação de Criação de Ligas | **Funcional**: interface com 4 entradas, 1 saída e botão "Fundir liga" (servidor valida e consome; usa as receitas da Fase 2). Desde a Fase 4 é um cadinho: precisa estar **diretamente sobre uma forja acesa** que atinja a chama e a temperatura da receita. |
| Bigorna, Bancada de Modelagem, Tanque de Têmpera, Forno de Tratamento Térmico, Estação de Polimento, Pedra de Amolar, Bancada de Gravação, Estação de Acabamento | **Apoiam uma peça**: clique com um item para colocar, mão vazia para recolher; a peça é desenhada sobre o bloco, salva no mundo, sincronizada no multiplayer e dropada ao quebrar. A função de cada estação (calor, martelamento, têmpera, polimento…) entra nas Fases 4–7. |

### Testar a Fase 3

1. Na aba criativa **Forja Suprema**, pegue as estações e coloque-as no mundo.
2. Clique com um lingote em uma estação (ex.: Bigorna): ele aparece sobre o bloco. Clique com a mão vazia para pegá-lo de volta. Quebre a estação com uma peça: ela cai no chão.
3. Abra a **Estação de Criação de Ligas** e coloque 3 lingotes de cobre e 1 de estanho nas entradas. Feche, reabra e reinicie o mundo: o conteúdo permanece. (Para fundir de verdade é preciso uma forja acesa embaixo; veja o teste da Fase 4. O comando `/forjasuprema alloy` continua sem exigir calor, só para depuração.)
4. Em modo sobrevivência, confira as receitas de crafting das estações no livro de receitas.

## O que existe hoje (Fase 4)

**Sistema térmico completo, simulado só no servidor.**

- **5 forjas** (Aprendiz, Ferreiro, Avançada, Mestre, Lendária), cada uma com teto de chama, bônus de chama da estrutura, teto de temperatura, retenção de calor e capacidade de combustível:

  | Forja | Chama máx. | Bônus | Temp. máx. | Esfria (1000→100 °C) |
  |---|---|---|---|---|
  | Aprendiz | 2 | +0 | 900 °C | ~1 min |
  | Ferreiro | 3 | +0 | 1250 °C | ~1,7 min |
  | Avançada | 3 | +1 | 1400 °C | ~2,6 min |
  | Mestre | 4 | +1 | 1600 °C | ~5 min |
  | Lendária | 4 | +1 | 2100 °C | ~10 min |

- **Combustíveis por datapack** (`forjasuprema:fuel`): intensidade, tempo de queima, eficiência térmica e temperatura máxima. Chama final = intensidade do combustível + bônus da forja, limitada pelo teto dela.

  | Combustível | Intensidade | Queima | Eficiência | Temp. máx. |
  |---|---|---|---|---|
  | Graveto | 1 (fraca) | 10 s | 0,6 | 500 °C |
  | Carvão vegetal | 2 (moderada) | 80 s | 0,9 | 1000 °C |
  | Carvão mineral | 3 (forte) | 120 s | 1,0 | 1550 °C |
  | Vara de blaze (especial) | 4 (intensa) | 240 s | 1,3 | 2000 °C |

- **Peças aquecem na forja** e guardam a temperatura no item (esfriam sozinhas, sem custo de processamento). O tooltip mostra a temperatura e o estado: fria demais, pronta para forjar, superaquecida ou derretida. Acima da janela de forjamento a peça acumula **dano de queima** (guardado para a qualidade, Fase 8); ao passar do ponto de fusão ela vira **escória**. Cuidado: o estanho derrete a 232 °C, então vira escória quase na hora em uma forja quente.
- **Regra fundamental**: `HeatRequirement` verifica a chama **antes** da temperatura. Chama fraca falha mesmo com a temperatura certa. A Estação de Ligas já usa essa regra; a têmpera e os demais tratamentos (Fase 6) usarão a mesma.
- **Estação de Ligas sobre a forja**: bronze exige chama 3 e 1100 °C (forja de Ferreiro com carvão mineral); aço exige chama 4 e 1500 °C (forja de Mestre).

### Testar a Fase 4

1. Pegue uma **Forja de Ferreiro** e carvão mineral na aba criativa. Clique com o carvão na forja (abastece) e depois com um **isqueiro**: a forja acende (luz, fumaça e brasas).
2. Clique na forja com a mão vazia (ou agachado) para ver `Forja | Chama | °C | Combustível`. Aguarde ~15 s: a temperatura sobe até 1250 °C.
3. Clique com um **lingote de ferro**: ele fica sobre a forja. Retire com a mão vazia e passe o mouse no lingote: a temperatura aparece no tooltip e cai com o tempo. Na Forja de Ferreiro o ferro esquenta até 1250 °C, que é o limite da janela de forjamento dele, então não queima. Para ver o superaquecimento, repita na **Forja de Mestre** com carvão mineral (alvo 1550 °C): o ferro passa de 1250 °C e acumula dano de queima (aparece no tooltip) e, ao atingir 1538 °C, vira **escória**.
4. Coloque a **Estação de Criação de Ligas diretamente sobre a forja acesa**, monte 3 cobre + 1 estanho e clique em **Fundir liga**: só funciona com a forja a 1100 °C ou mais. Com a forja fria, apagada ou de Aprendiz, aparece a mensagem de chama ou temperatura insuficiente e nada é consumido.
5. Comandos: `/forjasuprema fuel list` e `/forjasuprema fuel info forjasuprema:coal`.
6. Testes automáticos: `gradle test` (matemática térmica e regra de chama).

## O que existe hoje (Fase 5)

- **Martelamento na bigorna**: com o **Martelo de Ferreiro** na mão, clique numa **Bigorna** que tenha uma peça apoiada. Abre um minigame direcional: um marcador vai e volta na barra; aperte a seta indicada (setas/WASD) quando ele estiver na zona. Agachado, o martelo troca o formato (chapa, barra, pregos).
- **Foles**: clique numa forja **acesa** com os **Foles**; minigame de ritmo (Espaço/clique). A nota acelera o aquecimento por alguns segundos, sem elevar o teto de temperatura nem de chama.
- **Receita de forjamento** (`forjasuprema:forging`, datapack): `input`, `result`, `shape`, `strikes`, `complexity`. O resultado grava o metal (`metal_part`) e as notas por etapa (`stage_scores`: modelagem e controle térmico), exibidas no tooltip. A classificação de qualidade usará essas notas na Fase 8.
- **Dificuldade** depende da maleabilidade, do tier do metal, da complexidade da peça e da qualidade do martelo (bônus de nível entra na Fase 9). A **temperatura** da peça reduz a precisão: abaixo da janela de forjamento a eficiência cai; muito fria (200 °C abaixo do mínimo) a sessão é cancelada.
- **Servidor decide tudo**: o cliente só envia "bati no instante X". O servidor exige índice na ordem, intervalo mínimo entre golpes, instante dentro de ±8 ticks do relógio dele, distância, ferramenta e peça inalteradas, e expira a sessão. Uma sessão por jogador. `ProcessingMode.AUTOMATED` (Create, Fase 10) nunca abre minigames.
- **Limite conhecido**: um bot que leia o padrão e calcule o instante certo não se distingue de um jogador hábil por regras de servidor.

### Testar a Fase 5

1. Aqueça um lingote de ferro na Forja de Ferreiro (como na Fase 4) até ~900 °C ou mais e retire-o.
2. Apoie o lingote na **Bigorna** (clique com o lingote). Com o martelo, clique na bigorna: abre o minigame. Acerte os golpes; ao terminar, a peça vira uma **Chapa de Ferro** (tooltip mostra "Modelagem: N%").
3. Repita agachado com o martelo para trocar para barra ou pregos. Com o lingote frio, a bigorna recusa ("Fria demais").
4. Numa forja acesa, use os **Foles**: com boa nota a temperatura sobe mais rápido (compare o tempo para atingir o alvo).
5. Feche a tela no meio (Esc) ou afaste-se: a sessão é cancelada e a peça permanece intacta.
6. Testes: `gradle test` (matemática de golpes, dificuldade, gerador de padrão, sessão).

## O que existe hoje (Fase 6)

Os tratamentos são **determinísticos** (sem sorte): a nota vem de quão bem as condições foram mantidas, e **chama ou temperatura insuficientes fazem o tratamento falhar, sempre**, antes de qualquer consideração de desempenho. Cada metal declara em `treatments` o que aceita (bronze só recozimento e têmpera, cobre só recozimento, etc.).

| Técnica | Onde | Exigência | Efeito |
|---|---|---|---|
| **Têmpera** | Tanque de Têmpera (com água) | peça com temperatura ≥ mínima de forjamento do metal | + dureza (até +50%), + fragilidade; melhor logo acima do mínimo e dentro da janela, pior se superaquecida |
| **Recozimento** | Forno de Tratamento sobre forja acesa | chama ≥ 2, forno entre 80% do mínimo e (fusão − 50 °C); manter 30 s | zera dureza e fragilidade |
| **Revenimento** | idem | peça já temperada; chama ≥ 1; forno **suave**, entre 30% e 60% do mínimo (ex.: aço 255–510 °C, alcançável com galhos); 20 s | corta ~70% da fragilidade, perde ~15% da dureza extra |
| **Cementação** | idem | chama ≥ 3, 1 carvão no forno, 60 s | + dureza em metais de baixa temperabilidade (ferro) |
| **Endurecimento superficial** | idem | chama ≥ 3; 15 s | + dureza moderada, quase sem fragilidade |
| **Soldagem** | Bigorna: peça apoiada + clique com outra do mesmo metal | **ambas** ≥ 92% do topo da janela de forjamento | gera *Lingote Soldado* (forja 2 chapas/barras/8 pregos) com nota de solda |

- **Forno**: agachado + mão vazia alterna o tratamento; carvão abastece carbono; uma peça de metal inicia. Se a forja sai da faixa, o tempo **não conta**; se o tempo total passa de 3× o necessário, o processo falha e a peça fica como estava. A nota do tratamento é a fração do tempo em condição correta.
- Forjar na bigorna (peça trabalhada a quente) **apaga** o endurecimento anterior, mas preserva as notas das etapas (solda etc.).
- **Ligas avançadas** (datapack): *Aço de Damasco* (2 aço + 1 ferro, chama 4 e 1500 °C: só Forja de Mestre ou superior) e *Bronze Real* (3 bronze + 1 ouro, chama 3 e 1100 °C).
- Os efeitos aparecem no tooltip (dureza extra, fragilidade, tratamentos, notas). A conversão em atributos de equipamento acontece na Fase 8.

### Testar a Fase 6

1. **Têmpera**: aqueça um lingote de aço (ou ferro) acima de 850/900 °C, vá ao Tanque de Têmpera com um balde de água (clique para encher) e clique com a peça. Veja "Dureza extra" e "Fragilidade" no tooltip. Com a peça fria: "precisa de pelo menos … °C" e nada acontece.
2. **Revenimento**: coloque o **Forno de Tratamento** sobre uma Forja de Aprendiz abastecida com **galhos** (alvo 500 °C), agachado alterne para *Revenimento*, e clique com a peça temperada: o tempo corre só enquanto o forno está entre 255 e 510 °C. Com chama insuficiente ou forno fora da faixa, a mensagem diz o requisito. Sem têmpera prévia, é recusado.
3. **Recozimento**: com carvão vegetal (chama 2), alterne para *Recozimento*; ao final dureza e fragilidade voltam a zero.
4. **Soldagem**: apoie um lingote de ferro quente (≥ 1150 °C) na bigorna e clique com outro lingote de ferro quente: vira *Lingote Soldado*.
5. `gradle test` roda as regras de tratamento e de solda.

## Compilar e testar

Requer JDK 21 e Gradle 8.8+.

```bash
gradle build          # gera build/libs/forjasuprema-0.1.0.jar (roda os testes)
gradle test           # só os testes unitários
gradle runClient      # abre o Minecraft de desenvolvimento
gradle runServer
```

Para gerar o wrapper (uma vez): `gradle wrapper --gradle-version 8.14.3`, depois use `./gradlew`.

Teste da Fase 1: abra o jogo, confirme o mod na lista de mods, procure a aba criativa **Forja Suprema** e o item **Martelo de Ferreiro**. Com Create instalado, o log deve mostrar "Create detectado: integração pendente (Fase 10)". Sem Create, o jogo deve iniciar sem erros.

## Licença

Todos os direitos reservados (provisório, definir licença antes de publicar).
