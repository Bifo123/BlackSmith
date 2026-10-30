# Forja Suprema (blacksmith)

Mod de **ferraria, metalurgia e artesanato** para Minecraft **1.21.1 (NeoForge)**.
O jogador começa como aprendiz e evolui até forjador lendário: aquece, martela, trata termicamente, acaba e personaliza equipamentos. Integração opcional com **Create** para automação (sem minigames).

## Estado do projeto

| Fase | Descrição | Status |
|---|---|---|
| 1 | Arquitetura e esqueleto do projeto | ✔ entregue |
| 2 | Materiais e metalurgia básica | pendente |
| 3 | Blocos e estações | pendente |
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
