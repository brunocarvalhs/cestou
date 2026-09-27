# Persona Action Plan — Cestou (How-Much)

Status: Final — Fase "Agora" já tem 1 spec pronta (`item-add-authorship`); as demais aguardam spec própria antes de Design.
Companion doc: complementa `.specs/MVP-ROADMAP.md` (que cobre bloqueadores de lançamento/compliance — merge de branch, contas, Play Store). Este documento cobre evolução de **produto**, orientada por persona.
Last updated: 2026-09-04 — não reverificado desde então; ver `.specs/STATE.md` (entrada de 2026-09-21)
para o estado mais recente do projeto.
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

## Monetização (assinatura Pro) — avaliada, não priorizada agora

Status: Avaliada em 2026-09-27 pelo `pm`, a pedido do bruno. **Decisão: adiar.** Não entra em
Agora/Próximo/Depois do backlog acima nem vira spec ainda. Registrado aqui em vez de descartado
porque a análise que motivou a pergunta é válida e deve ser retomada assim que as precondições abaixo
forem satisfeitas.

**Contexto avaliado:** um "teste de invalidação" (o usuário consegue o mesmo resultado com uma
ferramenta genérica de fora, tipo ChatGPT/planilha/WhatsApp, com esforço comparável?) apontou 5
candidatos a feature paga: (1) IA escrevendo direto na lista compartilhada — hoje grátis, viraria
gatilho de paywall; (2) comparação de preço real via histórico próprio (`real-price-history`, item 4
acima); (3) relatório mensal de gastos + exportação (`monthly-spend-report`, item 5 acima); (4)
colaboração/compartilhamento com limite de membros/listas no grátis e "plano família" ilimitado no
pago; (5) Wear OS como add-on pago.

**Por que adiar, não recusar:**

- **Base real de usuários é minúscula.** Só ~2 contas reais confirmadas usam o app; os 950 testers do
  Open testing são opt-ins, não uso comprovado (ver nota do projeto sobre isso). Construir
  `core:billing` + `feature:subscription` + gating por `IsProUserUseCase` é investimento de engenharia
  não-trivial para uma base que ainda não provou reter ou completar o loop core, muito menos pagar por
  ele.
- **Nenhum piso do `.specs/BETA-KPI.md` foi lido ainda.** Os 7 KPIs bloqueantes (ativação ≥60%,
  conclusão de compra ≥50%, sucesso de join ≥70%, retenção D7 ≥25%, crash-free ≥97%, fricção de login
  <20%, canário de telemetria = 0 ocorrências) definem o piso mínimo para sequer considerar o app
  "saudável" com o cohort atual — nenhum foi reportado como medido em nenhum documento revisado nesta
  passada. Cobrar antes de saber se as pessoas completam o fluxo grátis é inverter a ordem: primeiro
  prova-se que o produto retém, depois se pergunta se ele monetiza.
- **Risco direto de persona em cima do próprio objetivo do beta.** Um paywall mal posicionado é
  fricção nova por definição — e fricção nova é exatamente o que os KPIs de ativação/retenção do beta
  estão tentando medir sem ruído. Introduzir cobrança agora contamina a leitura dos próprios números
  que decidiriam se vale a pena seguir.
- **Trave de billing sem validação server-side é uma exceção normalmente aceitável (base pequena),
  mas para dinheiro de verdade o cálculo muda.** O racional "base pequena, risco aceitável" que
  justifica outras simplificações do projeto (ex.: notificações client-side por falta de Cloud
  Functions) não se estende automaticamente a pagamento — é uma decisão do tech-lead/bruno assumirem
  esse risco especificamente para billing, não uma herança automática do resto do app.

**Cruzamento com as 12 personas (`.claude/skills/customer-personas/SKILL.md`):**

| Persona | Leitura | Por quê |
|---|---|---|
| **Eduardo** (economiza centavo) | Serve — pagaria pela versão *real* de comparação de preço (#2) | Bate direto com "rejeita qualquer fluxo que ainda exija calculadora"; mas cobrar pela versão hoje FAKE (`PRICE_ECONOMICO` fixo) seria vender algo que não existe — teria que ser a versão real primeiro, grátis ou paga |
| **Rafael** (revendedor) / **Anderson** (MEI pizzaria) | Serve — candidatos mais fortes para relatório mensal + exportação (#3) | É literalmente o trabalho manual em planilha que eles já fazem hoje; substituir isso com esforço comparável zero é o caso mais limpo de "vale cobrar" do lote |
| **Dona Marlene** | Ambíguo/risco | Valoriza comparação de preço e total sempre visível — mas seu contexto é orçamento fixo repassado pelo marido aposentado, sem folga; cobrar por algo que hoje é grátis e que ela já depende para não estourar o caixa é o tipo de mudança que pode alienar quem mais valoriza a feature original |
| **Dona Célia** | Atrapalha | Rejeita "qualquer feature que exija entender um conceito novo antes de usar" — uma tela de "assinatura Pro"/paywall É esse conceito novo; risco real de abandono se aparecer no caminho dela, mesmo que ela nunca fosse converter |
| **Marina** | Atrapalha se mal posicionado | Rejeita "qualquer fluxo com passos extras antes de ver o total do carrinho" — um paywall/interstitial no caminho do fluxo básico de compra quebra exatamente essa regra, independente do que está sendo vendido |
| **Lucas / Bianca-e-Diego** | Risco alto se #4 for mal calibrado | Rejeitam "qualquer solução/feature que só funcione bem com um único usuário" — um limite de membros/listas mal calibrado no plano grátis ataca direto o diferencial que os trouxe ao app; cap tem que ser folgado o bastante para nunca tocar o uso real deles (2-3 pessoas) |
| **Juliana** | Não se aplica ainda | `pantry-capture` (item 6, "Depois") ainda está em fase de spike, sem confirmação de que funciona — monetizar uma feature não validada é prematuro por si só, independente do resto |
| Demais (Camila-e-Pedro, Rodrigo, Yasmin) | Neutro a levemente-atrapalha | Não pagariam por nenhum dos 5 candidatos hoje, mas também não são o alvo primário de nenhum — risco é só se a IA grátis que eles já usam (#1) virar paga sem aviso, lido como perda de algo que já tinham |

**Ordem de prioridade, SE e QUANDO for retomado** (não é backlog ativo — é a resposta à pergunta "por
onde começar" para quando as precondições abaixo forem satisfeitas):

1. **Comparação de preço real (#2)** — já é o próximo item do backlog de produto (`real-price-history`,
   item 4 acima) independente de monetização; a versão fake atual precisa ser corrigida de qualquer
   forma. Construir a versão real primeiro resolve o problema de honestidade e só depois vira
   candidato a paywall.
2. **Relatório mensal + exportação (#3)** — reaproveita a agregação de histórico de #2 (mesma lógica já
   documentada no item 5 acima), e é o caso mais limpo do teste de invalidação (Rafael/Anderson não
   conseguem o mesmo resultado sem recriar manualmente o que o app já sabe).
3. **Limite de colaboração / plano família (#4)** — última a mexer, e com cautela: cap tem que ser
   calibrado para nunca tocar o uso real de Lucas/Bianca-e-Diego (grupos de 2-3), só limitar escala
   incomum (famílias grandes, muitas listas simultâneas).
4. **Paywall sobre a IA que já escreve na lista (#1)** — mais arriscado dos 5 porque já é grátis e usado
   por várias personas indiretamente; virar pago depois de ter sido oferecido grátis lê como
   bait-and-switch. Se for feito, via limite de uso generoso, não bloqueio duro.
5. **Wear OS pago (#5)** — última prioridade; nenhuma das 12 personas cita Wear OS como algo que
   valoriza, e hoje é só paridade mínima — monetizar algo sem sinal de demanda nem maturidade de
   feature é o candidato mais fraco do lote.

**Precondições para tirar isso de "adiado" e virar spec real** (todas, não algumas):

1. Os 7 KPIs bloqueantes de `.specs/BETA-KPI.md` lidos pelo menos uma vez com dados reais do cohort
   (mesmo que o resultado seja "abaixo do piso, precisa de outra rodada" — o ponto é ter o dado, não
   que ele seja bom).
2. G9 (merge de PR aberto) confirmado com o tech-lead/bruno — não é sobre reabrir o fix, é sobre saber
   se o app que os beta testers usam hoje já tem a lista confiável que teoricamente estão pagando por
   melhorar depois.
3. F0.3 (Maestro) e F2.2 (Google Sign-In em device real) executados pelo menos uma vez — cobrar em
   cima de um fluxo de compra/login nunca exercitado ponta a ponta é apostar dinheiro real numa base
   não testada.
4. Base de usuários reais (não opt-ins) crescida a um tamanho onde "quem pagaria" deixa de ser uma
   pergunta hipotética sobre personas fictícias e passa a ter alguém de verdade para perguntar.

Até essas quatro condições fecharem, `core:billing`/`feature:subscription` fica registrado aqui como
decisão avaliada e adiada, não como item de backlog — evita que a ideia se perca, mas também evita
gastar ciclo de design/engenharia numa aposta que os próprios dados do beta ainda não sustentam.

## Nota sobre feedback real de beta testers

Desde que o beta foi ao ar (950 usuários em Open testing, ver `.specs/BETA-LAUNCH-PLAN.md`), o
primeiro sinal negativo real chegou pelo canal "Feedback de teste" do Play Console: um comentário de
1★ dizendo apenas "difícil" (Felipe Morais, 07/09/2026), sem detalhe suficiente pra mapear contra uma
persona específica deste documento ainda. Registrado como **G17 em `.specs/MVP-ROADMAP.md`**, seção
"Beta feedback triage", com plano de follow-up (aguardar resposta dele; se não vier, sessão de
usabilidade ou revisão heurística do onboarding). Esse mesmo documento também define o processo daqui
pra frente: todo feedback de beta respondido que aponte um problema real de UX/funcionalidade vira um
gap ali, não só uma resposta no console. Um comentário positivo (5★, Mayra Santos) foi registrado como
sinal de validação, sem ação de produto necessária.
