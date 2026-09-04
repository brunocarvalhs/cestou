# Persona Action Plan — Cestou (How-Much)

Status: Final — Fase "Agora" já tem 1 spec pronta (`item-add-authorship`); as demais aguardam spec própria antes de Design.
Companion doc: complementa `.specs/MVP-ROADMAP.md` (que cobre bloqueadores de lançamento/compliance — merge de branch, contas, Play Store). Este documento cobre evolução de **produto**, orientada por persona.
Last updated: 2026-09-04
Origem: análise das 12 personas do skill `customer-personas`, spec `.specs/features/item-add-authorship/spec.md`, review de UX/acessibilidade, e uma rodada PM ↔ Tech Lead.

---

## Objetivo final

O Cestou já tem as features "core" funcionando (compra, compartilhamento, IA, scanner). O que falta
não é mais funcionalidade — é fazer o que já existe (e o que vem a seguir) ser **entendido e usado
sem esforço** pelas pessoas reais por trás das 12 personas: da Dona Célia, que rejeita qualquer tela
com jargão ou passo escondido, ao Eduardo, que só considera um problema "resolvido" se ele não
precisa mais pegar a calculadora à parte. O critério de sucesso deste plano não é quantas features
saem, é quantas personas passam de "neutro" ou "não atende" para "atende" — e nenhuma delas passa a
achar o app mais confuso no processo. Cada item abaixo só entra em "Agora" ou "Próximo" se reduzir
fricção líquida, não só resolver um problema trocando-o por outro.

---

## Reconciliação com o Tech Lead

| Ponto do Tech Lead | Decisão | Por quê |
| --- | --- | --- |
| Ordem Agora/Próximo/Depois, quase toda | **Aceito** | Sem contraponto técnico relevante que mude a priorização por persona. |
| `item-row-affordances` seguro logo após IAA, sem colisão real de código | **Aceito** | Confirma a intuição original — specs continuam separadas, mas o ciclo de trabalho fica junto. |
| Custo de agregação de histórico entre listas no plano Spark — minha cautela não se confirmou | **Aceito, ajusto o plano** | Removo a barreira de custo que eu tinha usado pra manter #3/#4/#6 conservadoramente espaçados; isso permite fundir #3 em #4 (próximo ponto) sem risco técnico extra. |
| Fundir "fundação de histórico agregado" dentro de "comparação de preço real", como uma spec só | **Aceito** | Argumento é mais forte que o meu original: histórico isolado não tem Independent Test observável por nenhuma persona (quebra convenção do repo), e comparar contra o histórico do próprio usuário não é só melhor UX — é a única versão honesta e viável sem orçamento pra fonte de preço externa. Isso vira uma iniciativa só no backlog final. |
| Promover vínculo receita↔lista (#5) para "Agora" | **Aceito** | É o item mais barato do backlog inteiro, isolado (não toca nos mesmos arquivos de #1/#2), e a persona que ele atende (Yasmin) hoje não tem nenhum outro item no backlog cobrindo ela — custo baixo, ganho de cobertura real. |
| Foto de despensa (#7) precisa de spike de validação antes de qualquer spec | **Aceito** | Achado concreto (ML Kit só rotula genérico, Gemini agent hoje só aceita texto) muda a natureza do item: não é "escrever uma spec e implementar", é "validar se dá pra fazer direito antes de prometer". Incorporado como pré-requisito explícito, não como spec direta. |
| Addendum técnico do IAA-01 (resolver perfis uma vez no ViewModel, getters computados no padrão de `Product.total`, ponto único de construção de `ProductActivity`) | **Aceito, fica registrado para o `design.md`** | É orientação de implementação, não muda o backlog de produto — mas fica documentado aqui pra não se perder até `item-add-authorship` entrar em Design. |
| Débito técnico (grafo de dependência real vs. documentado) e bug de reordenar lista (G9/F2.4) | **Aceito como trilha separada** | Já registrado em `MVP-ROADMAP.md` pelo Tech Lead; não é decisão de produto, então não entra nas fases abaixo — só citado na nota final. |

---

## Backlog final

### Agora

| Item | Persona(s) | Problema em 1 frase | Esforço/risco | Por que agora |
| --- | --- | --- | --- | --- |
| **1. Item Add Flow & Authorship** (`item-add-authorship`, IAA-01..03 — spec pronta) | Lucas, Bianca-e-Diego (direto); melhora Marina, Dona Marlene, Dona Célia, Camila-e-Pedro, Rodrigo | Adicionar item cai num chat de IA por padrão e leva 2 abas aninhadas pra chegar num item comum; ninguém sabe quem adicionou ou editou o quê numa lista compartilhada | Baixo — spec fechada, validada por review de UX independente; addendum técnico do Tech Lead (perfis resolvidos 1x no ViewModel, getters computados, ponto único de escrita de `history`) já anotado pro `design.md` | Única iniciativa pronta pra Design agora; tudo mais depende de não reabrir os mesmos arquivos depois |
| **2. Affordances visíveis de editar/apagar item e lista** (`item-row-affordances` — spec a fazer) | Dona Célia (direto); sem downside pras demais | Editar/apagar hoje só existe via long-press escondido (`ProductListItem.kt:72-76`) ou swipe (`:56-66`), mesmo padrão em `ShoppingItem.kt:83-86` | Baixo — Tech Lead confirmou que não colide de fato com #1 no código, mesmo tocando arquivos próximos | Sequenciar no mesmo ciclo de #1 evita reabrir `ProductListItem.kt`/`ShoppingItem.kt` duas vezes |
| **3. Vínculo lista ↔ receita de origem** (`recipe-list-origin` — spec a fazer) | Yasmin | `Shopping` não guarda de qual receita ela veio; a usuária perde a referência entre ir ao mercado e voltar pra casa | Muito baixo — campo nullable a mais em `Shopping`/`ShoppingModel`, mesmo padrão já usado por `budget`; sinal verde técnico explícito | Promovido de "Próximo": é o item mais barato do backlog inteiro e a única cobertura hoje pra uma persona sem nenhum outro item no plano |

### Próximo

| Item | Persona(s) | Problema em 1 frase | Esforço/risco | Por que aqui |
| --- | --- | --- | --- | --- |
| **4. Comparação de preço real, baseada no histórico do próprio usuário** (`real-price-history` — fusão do que antes eram dois itens separados) | Dona Marlene, Eduardo | `PriceComparisonUseCase` hoje devolve preços 100% inventados (`PRICE_ECONOMICO = 10.50` fixo no código) — a IA já "responde" uma comparação falsa se alguém pedir | Médio — inclui construir a agregação de histórico entre listas do usuário (confirmada barata no Firestore mesmo no plano Spark) como parte desta mesma spec, porque é o que dá o Independent Test observável | Não pode ficar em "Agora" só porque é urgente corrigir o dado falso: precisa da agregação de histórico primeiro, e essa agregação só se justifica como spec junto com um resultado visível pra persona |
| **5. Relatório mensal de gastos + exportação** (`monthly-spend-report` — spec a fazer, depois de #4) | Rafael, Anderson | Sem agregação entre listas finalizadas, os dois continuam somando manualmente (calculadora ou planilha) o que já está no app | Médio — reaproveita a agregação que #4 constrói, reduzindo o próprio custo; maior risco aqui é de escopo (virar dashboard em vez de lista + CSV simples) | Sequenciado depois de #4 de propósito: usa a mesma infra, então sai mais barato construído em cima dela do que em paralelo |

### Depois

| Item | Persona(s) | Problema em 1 frase | Esforço/risco | Por que depois |
| --- | --- | --- | --- | --- |
| **6. Captura sem digitar (foto de despensa) + insight de consumo** (`pantry-capture` — **spike antes de spec**) | Juliana | Ela tira foto do armário em casa e tenta lembrar de cabeça o que falta; hoje o app só lê etiqueta de preço em loja, não múltiplos itens numa foto de casa | Alto, e hoje **desconhecido** — achados do Tech Lead: `MlKitImageAnalyzerService` só devolve um rótulo genérico por imagem, `GeminiAiAgent.sendMessage()` hoje só aceita texto, sem entrada de imagem | Maior risco de *adicionar* confusão do backlog inteiro (reconhecimento errado é pior que a usuária confiar na própria memória) — não vira spec até um spike validar se dá pra reconhecer bem múltiplos itens numa foto real, com qual abordagem (ML Kit vs. Gemini Vision) |

---

## Nota sobre a trilha técnica paralela

Em paralelo a este plano de produto, o Tech Lead abriu uma trilha própria de débito técnico: o
grafo de dependência real entre os módulos de feature (cart/shopping/chat/ai-agent importando
direto de `feature/settings`) diverge da arquitetura documentada, e um bug real já em produção —
reordenar lista por arrastar não persiste no Firestore (no-op silencioso) — foi registrado como
**G9/F2.4 em `.specs/MVP-ROADMAP.md`**. Nenhum dos dois entra nas fases acima: não são decisões de
priorização por persona, são correções técnicas que seguem o processo já estabelecido no roadmap de
lançamento.
