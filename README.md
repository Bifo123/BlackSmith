# Forja Suprema (blacksmith)

Mod de **ferraria, metalurgia e artesanato** para Minecraft **1.21.1 (NeoForge)**.
O jogador começa como aprendiz e evolui até forjador lendário: aquece, martela, trata termicamente, acaba e personaliza equipamentos. Integração opcional com **Create** para automação (sem minigames).

## Estado do projeto

| Fase | Descrição | Status |
|---|---|---|
| 1 | Arquitetura e esqueleto do projeto | ✔ entregue |
| 2 | Materiais e metalurgia básica | ✔ entregue |
| 3 | Blocos e estações | ✔ entregue (comportamento de cada estação vem nas fases seguintes) |
| 4 | Temperatura e intensidade da chama | pendente |
| 5 | Minigames | pendente |
| 6 | Técnicas metalúrgicas | pendente |
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
| Estação de Criação de Ligas | **Funcional**: interface com 4 entradas, 1 saída e botão "Fundir liga" (servidor valida e consome; usa as receitas da Fase 2). Sem exigência de calor até a Fase 4. |
| Fornalha de Ferreiro, Bigorna, Bancada de Modelagem, Tanque de Têmpera, Forno de Tratamento Térmico, Estação de Polimento, Pedra de Amolar, Bancada de Gravação, Estação de Acabamento | **Apoiam uma peça**: clique com um item para colocar, mão vazia para recolher; a peça é desenhada sobre o bloco, salva no mundo, sincronizada no multiplayer e dropada ao quebrar. A função de cada estação (calor, martelamento, têmpera, polimento…) entra nas Fases 4–7. |

### Testar a Fase 3

1. Na aba criativa **Forja Suprema**, pegue as estações e coloque-as no mundo.
2. Clique com um lingote em uma estação (ex.: Bigorna): ele aparece sobre o bloco. Clique com a mão vazia para pegá-lo de volta. Quebre a estação com uma peça: ela cai no chão.
3. Abra a **Estação de Criação de Ligas**, coloque 3 lingotes de cobre e 1 de estanho nas entradas e clique em **Fundir liga**: 4 lingotes de bronze aparecem na saída. Feche, reabra e reinicie o mundo: o conteúdo permanece.
4. Em modo sobrevivência, confira as receitas de crafting das estações no livro de receitas.

## Compilar e testar

Requer JDK 21 e Gradle 8.8+.

```bash
gradle build          # gera build/libs/forjasuprema-0.1.0.jar
gradle runClient      # abre o Minecraft de desenvolvimento
gradle runServer
```

Para gerar o wrapper (uma vez): `gradle wrapper --gradle-version 8.14.3`, depois use `./gradlew`.

Teste da Fase 1: abra o jogo, confirme o mod na lista de mods, procure a aba criativa **Forja Suprema** e o item **Martelo de Ferreiro**. Com Create instalado, o log deve mostrar "Create detectado: integração pendente (Fase 10)". Sem Create, o jogo deve iniciar sem erros.

## Licença

Todos os direitos reservados (provisório, definir licença antes de publicar).
