# Task: Migrar o layout de todas as telas para o canvas "Subtotal — nova identidade"

- **Slug:** layout-nova-identidade
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-15
- **Status:** ready
- **Versão-alvo:** próxima major (a breaking já está marcada em `2205cc75 feat(ui)!`; versão e CHANGELOG ficam com o processo de release)
- **Tipo de mudança:** refactor (visual, sem regra de negócio nova)
- **Impacto público (módulo):** additive (`core/ui` ganha componentes públicos; nenhum símbolo público removido nesta task)

## 1. Contexto e Motivação

- O branch `feat/nova-identidade-breaking` já trocou tokens, fontes e raios (`2205cc75`) e fez um *reskin* raso das telas (`b1aa4b4e`, `02761f0f`, `61d858ed`: cor de título, borda, raio). A **estrutura** das telas ainda não segue o canvas.
- Pedido do usuário: "crie um plano pra migrar todos os layout para o layout igual a esse proposto" — canvas `https://claude.ai/artifact/NU21sXCB2GHyxLug5FYoBr` (30 artboards, 390×844, claro e escuro).
- Há trabalho em andamento **não commitado** que já é desta task (Onda 0/1): `CestouBottomNavigation.kt` (4 abas Listas/Gastos/Avisos/Perfil), `CartBottomBar.kt`, `ProductListItem.kt`, `CartScreen.kt`, `ShoppingScreen.kt` e o novo `FeaturedListCard.kt` (`feature/shopping/.../components/shopping/FeaturedListCard.kt:37`).
- Tokens claros já batem com o canvas (`core/theme/.../Color.kt:9-20`); tokens escuros divergem levemente (ver RN 1).

## 2. Escopo

**Inclui** (artboard → tela atual):

| Artboard | Tela / componente atual | Gap estrutural principal |
|---|---|---|
| `Screen` / `ScreenEscuro` | `ShoppingScreen.kt:69` | Header "Suas listas" + 3 botões 44 dp (entrar, avisos com badge, nova lista); card herói azul da lista em destaque com orçamento; bloco "Itens da vez" (3 itens com check); pilha de avatares; CTA navy "Continuar compra" |
| `DetalheLista` / `…Escuro` | `CartScreen.kt:102`, `CartBottomBar.kt`, `ProductListItem.kt` | Header voltar + título + "Compartilhada com…" + mais; card de progresso do orçamento; campo "Adicionar item" + botão de código de barras (`CartScreen.kt:372`); seções por categoria em caixa alta (`CartScreen.kt:309`); linha de item com "quem adicionou · qtd"; barra inferior branca "No carrinho · x de y" + CTA navy. **Remover o FAB** (`CartScreen.kt:351`) |
| `AdicionarItens` / `…Escuro` | `ProductScreen.kt:42` + `Suggestions.kt` | Campo de busca com borda primária e scanner embutido; chips de categoria; "Sugestões para hoje" com botão "+" 36 dp; aviso coral de duplicado (`ProductDuplicateCheckUseCase`); rodapé com 2 CTAs (foto IA / buscar) |
| `Historico` / `…Escuro` | `HistoryScreen.kt:37,92` | Card navy de resumo do mês; cards de lista finalizada com selo "Finalizada" e botões "Ver itens"/"Reutilizar"; rodapé com cadeado "listas finalizadas ficam bloqueadas" |
| `Orcamento` | `SpendingInsightsScreen.kt:51,109` | Título "Gastos"; segmento Semana/Mês; card herói com meta e restante; barras por semana (`MonthlySpendingChart`); "Por lista"; CTA "Editar meta mensal" |
| `Notificacoes` | `NotificationsScreen.kt:61,133` | Título + contador + "Marcar como lidos" (já existe `MarkAllNotificationsReadUseCase`); grupos Hoje/Esta semana (`GroupNotificationsUseCase`); card "Horário do lembrete" com atalho para `NotificationSettingsScreen` |
| `Compartilhar` | `ShareOptionsBottomSheet.kt` | Código grande + "Copiar código" + "Enviar convite" + QR + "Quem está na lista" (papel: Criador / Pode editar) |
| `EntrarLista` | `JoinListContent.kt` | Campo de código + "Escanear QR Code" + prévia "Você vai entrar em…" + CTA |
| `AssistenteIA` | `AiChatScreen.kt:91`, `ApprovalCard`, `ChatBubble` | Header com nome da lista + selo Pro; card de ação para aprovar (Ajustar / Aprovar) no padrão do canvas; campo "Pergunte algo sobre a lista" + botão de voz |
| `HistoricoAssistente` | `AiHistoryScreen.kt:57,132` | Campo "Buscar nas conversas"; grupos Hoje/Ontem/Esta semana; card com lista, última mensagem e hora; CTA "Nova conversa" no rodapé no lugar do FAB (`AiHistoryScreen.kt:81`) |
| `FotoIA` | fluxo de foto em `feature/products` (`ProductAnalysisConfirmation.kt`) | Tela escura (fundo `SubtotalNight`), visor tracejado, lista "Itens identificados" com % de confiança e check coral, CTA coral "Adicionar N itens" |
| `Configuracoes` / `…Escuro` | `SettingsScreen.kt:49,70` | Trocar o acordeão (`SettingsAccordionSection`) por seções fixas com rótulo e cards; linhas com valor à direita e `›` |
| `Assinatura` / `AssinaturaClaro` | `PaywallScreen.kt:48,165` | Fechar + "Restaurar compra"; título, benefícios com ícone; planos como `radio` (Anual / Mensal); CTA coral; rodapé da Google Play |
| `Relogio` | `wear/` + `feature/shopping/.../wear/screen/ShoppingWearScreen.kt` | Cartão redondo: nome da lista, total, "x de y itens · faltam R$", ação "Marcar próximo" |
| Navegação | `CestouBottomNavigation.kt` (WIP), `MainActivity.kt:249` | 4 abas; `rootRoutes` ainda lista `History` e não lista `SpendingInsights`/`Notifications` |

**Exclui:**
- `Comparador` / `ComparadorEscuro`: não existe feature de comparação de preços no código (nenhuma ocorrência de `PriceCompar*`). Vira spec própria.
- `Recorrente` / `RecorrenteEscuro`: depende de `sdd-063` e `sdd-064` (ambas `blocked`). Só o layout do formulário "Nova lista" existente (nome + orçamento + CTA) entra aqui.
- Linhas "Idioma" e "Moeda" de `Configuracoes`: não há preferência de idioma/moeda no app; não serão criadas linhas sem função.
- Artboards de marca (`Main`, `MainEscuro`, `Icon`, `Feature`, `Funcionalidades`, `FuncionalidadesEscuro`): referência, não tela. O ícone já foi trocado em `b1aa4b4e`.
- Telas sem artboard (auth, welcome, legal, licenças, release notes, feedback, force update, settings internas): só herdam os componentes da Onda 0; sem redesenho.
- Rename `Cestou*` → `App*` e inversão Screen/Content: pertencem ao refactor de componentes (planejado para depois da fila #180). Esta task **mantém o prefixo `Cestou`** para não misturar diffs.

## 3. Impacto Arquitetural

- **Camadas:** apenas `presentation` (composables, `UiState` de exibição) e `core/ui`/`core/theme`. Nenhuma mudança em `domain`, `data`, `di`. Navegação: só a lista de rotas raiz em `app`.
- **Padrão:** Clean Arch + MVI mantidos. Componentes novos são stateless; dados vêm do `UiState` já existente. Onde a tela precisar de um dado derivado novo (ex.: "3 primeiros itens não marcados" da Home), ele é calculado no ViewModel e exposto como `ImmutableList` / campo pronto — nunca dentro do composable.

**Novos arquivos (`core/ui/src/main/java/br/com/brunocarvalhs/howmuch/core/ui/components/`):**
- `CestouHeroCard.kt` — card 20 dp, variantes `Blue` (Home, Gastos) e `Night` (Histórico).
- `CestouCheckRow.kt` — linha de item 52–56 dp com check 24 dp (raio 8), título, subtítulo opcional, preço tabular; estado marcado = riscado + `SubtotalSlate`.
- `CestouSectionLabel.kt` — rótulo 12 sp, caixa alta, tracking 0.06em.
- `CestouPill.kt` — selo 999 dp (Pro, Finalizada, "Perto do limite", contadores).
- `CestouSegmented.kt` — segmento de 2–3 opções (Semana/Mês) com `role = Tab`.
- `CestouSearchField.kt` — campo 52 dp raio 14 com ícone e ação à direita (scanner / limpar).
- `CestouListCard.kt` — card branco, borda `outline`, raio 16, padding 16 (Histórico, Configurações, Compartilhar, Histórico do assistente).

**Alterados em `core/ui`/`core/theme`:**
- `CestouButton.kt:26` — novo parâmetro `style: CestouButtonStyle = Primary` com `Primary` (azul), `Strong` (navy, CTA de rodapé), `Accent` (coral, paywall/foto), `Outlined`.
- `Theme.kt:17` — ajuste dos tokens escuros (RN 1).
- `CestouBottomNavigation.kt` — finalizar o WIP.

```
            core/theme (tokens, Shapes, Typography)
                        │
            core/ui  ── CestouHeroCard · CestouCheckRow · CestouSectionLabel
                        CestouPill · CestouSegmented · CestouSearchField
                        CestouListCard · CestouButton(style) · CestouHeaderButton
                        CestouBottomNavigation (4 abas)
                        │
   ┌───────────┬────────┼─────────┬──────────┬─────────────┬──────────┐
 shopping     cart   products    chat     settings     subscription  wear
 (Home,       (Lista (Adicionar, (Assist., (Config.,    (Paywall)    (Relógio)
  Histórico,   aberta) Foto IA)   Histór.)  Perfil)
  Gastos,
  Avisos,
  Entrar)
                        │
                 app/MainActivity (rootRoutes + bottom bar)
```

- Hilt: sem mudança. Comunicação entre módulos: features dependem de `core/ui`, nunca umas das outras.

## 4. Contratos e Interfaces

Todos os componentes novos são `public` porque `core/ui` é consumido por outros módulos (mesma regra dos `Cestou*` atuais). Assinaturas (sem implementação):

```kotlin
enum class CestouHeroVariant { Blue, Night }
@Composable fun CestouHeroCard(variant: CestouHeroVariant, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)

@Composable fun CestouCheckRow(
    title: String, price: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, subtitle: String? = null,
)

@Composable fun CestouSectionLabel(text: String, modifier: Modifier = Modifier)

enum class CestouPillTone { OnPrimary, Neutral, Accent, Info }
@Composable fun CestouPill(text: String, tone: CestouPillTone, modifier: Modifier = Modifier)

@Composable fun <T> CestouSegmented(options: ImmutableList<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier)

@Composable fun CestouSearchField(
    value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null,
)

@Composable fun CestouListCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)

enum class CestouButtonStyle { Primary, Strong, Accent, Outlined }
// CestouButton(..., style: CestouButtonStyle = CestouButtonStyle.Primary)  — additive, default mantém o visual atual
```

**UiState (internal, por feature) — só campos de exibição:**
- `ShoppingListUiState`: `featured: FeaturedListUi?` com `pendingPreview: ImmutableList<ProductPreviewUi>` (máx. 3) e `members: ImmutableList<MemberUi>`.
- `HistoryUiState`: `monthSummary: MonthSummaryUi?` (total do mês corrente, nº de listas) — derivado de `GroupHistoryByMonthUseCase`.
- `SpendingInsightsUiState`: `period: InsightsPeriod` (`WEEK`, `MONTH`) + `SpendingInsightsIntent.SelectPeriod`. **Só entra se o use case atual suportar o recorte semanal; senão o segmento fica fora (Pergunta 2).**
- `AiHistoryUiState`: `query: String` + `AiHistoryIntent.Search(query)` com filtro local (sem I/O novo).

**NavKey:** nenhum novo, salvo se a Pergunta 1 decidir que `Compartilhar` vira tela (`data class ShareList(val shoppingId: String) : NavKey` em `core/navigation`).

**Analytics / remote-config / Firestore:** nenhum evento, chave ou coleção nova. Eventos existentes ficam nos mesmos gatilhos; quando um gatilho muda de componente (FAB → botão de rodapé), o evento migra junto com o mesmo nome.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Esquema escuro: fundo `#0A1226`, superfície `#131D36`, linha `#2A3858`, primário `#8FA8FF`, texto de apoio `SubtotalMuted #9AA4B8` | Alinhar ao canvas escuro: fundo `#0B1226`, superfície `#15203D`, linha `#2A3A63`, primário `#8FA9FF`, texto de apoio `#A9B8E6` | `core/theme/.../Color.kt:25-29`, `Theme.kt:17-40`; canvas `ScreenEscuro` |
| 2 | Cor dinâmica desligada para manter a marca | Mantida | `Theme.kt:84` |
| 3 | Barra inferior: Listas, Histórico, Perfil | Listas, Gastos, Avisos, Perfil (WIP). Histórico passa a ser aberto pelo card herói de Gastos e pelo Perfil | `CestouBottomNavigation.kt` (diff local) |
| 4 | Rotas raiz: `ShoppingList, History, AiChat, Profile, NewShopping` | Trocar `History` por `SpendingInsights` e incluir `Notifications`, para a barra aparecer e o "voltar" sair do app nas 4 abas | `MainActivity.kt:249` |
| 5 | Lista finalizada não pode ser alterada | Mantida; agora explícita no rodapé do Histórico ("ficam bloqueadas") | `HistoryScreen.kt:92`, `architecture/02-regras-de-negocio.md` |
| 6 | Aviso de item duplicado ao adicionar | Mesmo use case; muda só a apresentação (faixa coral inline em vez do diálogo/snackbar atual) | `ProductDuplicateCheckUseCase` |
| 7 | "Quem adicionou" vem de `Product.addedBy` (userId) | Exibido como nome do membro ("Ana · 6 un."), resolvido com os membros que o `CartViewModel` já carrega; userId desconhecido → "Você" se for o usuário atual, senão omitido | `core/domain/.../model/Product.kt:19`, `CartProductItem.kt` |
| 8 | Ação da IA só altera a lista após aprovação | Mantida; o card do canvas mantém Ajustar/Aprovar | `ApprovalCard` em `AiChatScreen.kt:398` |
| 9 | Compartilhar lista: limite free | Inalterado aqui (escopo de `sdd-062`); o selo "Pro" do artboard só aparece se o gate já existir | `.specs/sdd-062-compartilhar-lista-limite-free/tasks.md` |
| 10 | `testTag` são contrato com o Maestro | Nenhum valor de `testTag` muda; componentes novos repassam `Modifier` para o tag continuar no nó clicável | memória `feedback_maestro_testtag`, 220 `testTag(` em `feature/*` e `core/ui` |

## 6. Critérios de Aceitação

**Fidelidade visual (por tela da Seção 2):**
- Medidas do canvas: botões de header 44 dp raio 14; CTA 52 dp raio 14; cards 16 dp (listas) e 20 dp (herói); padding lateral 20 dp; espaçamento entre blocos 14–20 dp; título de aba em `headlineMedium` (Plus Jakarta 800, 26/22/20 sp conforme artboard).
- Valores monetários com `fontFeatureSettings = "tnum"`.
- Cada tela tem `@Preview` claro e escuro (`PreviewCestouScreens.kt`) com dados equivalentes aos do artboard.
- Contraste ≥ 4.5:1 nos textos de apoio, incluindo o `#A9B8E6` sobre `#15203D` no escuro.
- Alvos de toque ≥ 44 dp; ícones só com `contentDescription`; progresso com `semantics { progressBarRangeInfo }`.

**Performance:**
- Nenhum cálculo de lista no composable: prévia da Home, agrupamento por categoria, resumo do mês e filtro de busca saem do ViewModel em `Dispatchers.Default` quando a lista passar de algumas dezenas de itens.
- `LazyColumn`/`LazyVerticalGrid` com `key` estável (id do produto/lista) e `contentType` por tipo de linha (rótulo de seção × item).
- Coleções no `UiState` como `ImmutableList` (componentes recebem parâmetros estáveis; sem `List` mutável nos novos componentes).
- Formatadores de moeda criados uma vez (`remember(locale)`), nunca por item.
- Ícones vetoriais da barra montados uma vez (já em `object`, `CestouBottomNavigation.kt`); nada de `PathParser` por recomposição.
- Barra de progresso animada com `animateFloatAsState` lida em `drawBehind`/`graphicsLayer`, para não recompor o card inteiro.
- Sem I/O novo; nenhuma tela passa a observar fluxos que não observava.

**Feature toggles:** nenhum. A mudança é visual, vai junto com a major e não convive com o layout antigo (manter os dois dobraria a manutenção dos 220 `testTag`).

**Observabilidade:** eventos de Analytics existentes continuam disparando nos mesmos gatilhos; verificar no DebugView do GA4 os eventos de tela (`screen_view`) das 4 abas novas.

**Compatibilidade:** minSdk 28 (nenhuma API nova); Wear OS só no artboard `Relogio`; sem migração de dados.

## 7. Plano de Testes

Ordem de entrega em ondas, cada uma um PR para `feat/nova-identidade-breaking` (ou um commit cada, se o branch for um PR só):

- **Onda 0 — fundação:** tokens escuros; componentes novos em `core/ui`; `CestouButton(style)`; finalizar barra inferior; `rootRoutes` (`MainActivity.kt:249`).
- **Onda 1 — fluxo de compra:** Home, Lista aberta, Adicionar itens, Histórico (inclui o WIP de `CartScreen`, `ProductListItem`, `CartBottomBar`, `FeaturedListCard`).
- **Onda 2 — abas e social:** Gastos, Avisos, Perfil + Configurações, Compartilhar, Entrar em lista.
- **Onda 3 — IA:** Assistente, Histórico do assistente, Foto com IA.
- **Onda 4 — fechamento:** Paywall, Relógio, revisão do modo escuro em todas as telas, Maestro completo.

**Unitários:**
- `CoreUiComponentsTest.kt`: cada componente novo — renderização, `checked` → semântica `toggleableState`, `CestouSegmented` emite `onSelect` só para opção diferente, `CestouButton` com cada `style` respeita `enabled = false`.
- ViewModels: `ShoppingListViewModel` (prévia com no máximo 3 itens pendentes; lista sem itens → `featured.pendingPreview` vazio; nenhuma lista → `featured = null`), `HistoryViewModel` (resumo só do mês corrente; mês sem compras → `null`), `AiHistoryViewModel` (busca sem acento/caixa; query vazia devolve tudo), mapeamento "quem adicionou" (usuário atual → "Você"; userId fora dos membros → sem subtítulo).
- Negativos: falha de rede mantém o estado de erro atual em cada tela redesenhada; lista bloqueada (finalizada) não mostra o campo "Adicionar item".

**Instrumentados / UI:**
- Maestro: rodar os 14 fluxos de `.maestro/flows/` em pt-BR e EN a cada onda; ajustar só a navegação dos fluxos 02 e 04 (já alterados no diff local) para as abas novas, sempre por `testTag`.
- Screenshots manuais por artboard (claro/escuro) anexados ao PR para comparação lado a lado.

**Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Sem `@Suppress`: achado de Detekt (ex.: `LongMethod` em `CartScreen`) é resolvido extraindo composables.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Conflito com o refactor `Cestou*`→`App*` / Screen-Content (mesmos arquivos) | alta | rebase grande | Esta task entra antes; o refactor roda depois sobre o layout novo. Não renomear nada aqui |
| Fluxos Maestro quebram com a troca de abas e a remoção de FABs | média | CI vermelho | Manter `testTag`; o botão de rodapé herda o tag do FAB removido |
| Histórico perde descoberta ao sair da barra | média | queda de uso do Histórico | Entrada no card herói de Gastos e no Perfil; acompanhar `screen_view` de Histórico no GA4 após a release |
| Diferença de contraste no escuro com os tokens novos | baixa | acessibilidade | Validar 4.5:1 nas previews escuras antes do merge |
| `CartScreen.kt` (474 linhas) cresce e estoura Detekt | média | lint | Extrair header, card de orçamento e seção em arquivos de `components/` |
| Branch grande demais para revisão | alta | revisão lenta | Um commit por onda, diff só visual, screenshots no PR |

## 9. Perguntas em Aberto

Nenhuma bloqueante. Cada item abaixo tem um padrão já decidido no plano; quem for implementar segue o padrão, a menos que o maintainer diga outra coisa:

- [ ] **(não bloqueante) Compartilhar: tela ou bottom sheet?** Padrão: manter `ShareOptionsBottomSheet` expandido com o layout do artboard (código, QR, membros), sem `NavKey` novo. "Gerar novo código" só entra se já existir um use case de regenerar token.
- [ ] **(não bloqueante) Segmento Semana/Mês em Gastos.** Padrão: só entra se `GetCategorySpendingUseCase`/`MonthlySpendingChart` já tiverem recorte semanal; senão a tela fica em "Mês" e o segmento vira follow-up.
- [ ] **(não bloqueante) Comparador e Lista recorrente** ficam fora (Seção 2). Confirmar se o Comparador deve virar spec nova (`sdd-066`).
- [ ] **(não bloqueante) Lacuna de documentação:** `architecture/` não descreve o design system; registrar os componentes da Onda 0 em `architecture/05-decisoes-arquiteturais.md` no PR da Onda 0.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete o impacto (`additive`: só parâmetro com default e componentes novos).
- [x] Novo endpoint/remote-config: não há.
- [x] Novo cache: não há.
- [x] Perguntas em aberto exauridas (restantes têm padrão definido e não bloqueiam).
