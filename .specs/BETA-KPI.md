# Beta KPI — Piso mínimo de saúde do beta fechado (Cestou / How-Much)

Status: Draft — para revisão do `pm`.
Owner: `marketing`
Last updated: 2026-09-09

Companion de `.specs/BETA-LAUNCH-PLAN.md` (sequenciamento do trabalho) e `.specs/ANALYTICS-PLAN.md`
(inventário de eventos/funis, `data-engineer`). Cruzado com a seção "Beta Launch Priority"
(owned by `pm`) de `.specs/MVP-ROADMAP.md`. Não edita nenhum desses três arquivos.

## Para que serve este documento

Não é uma lista de métricas "legais de ter". É o **piso mínimo aceitável** para decidir, com dados
reais do cohort de beta testers, se o app está pronto para seguir do beta fechado para o lançamento
público — ou se precisa de mais uma rodada de correções antes disso. Cada KPI abaixo só usa eventos
que já existem em `AnalyticsEvents.kt` e já foram auditados/instrumentados pelo `data-engineer`
(ver `ANALYTICS-PLAN.md`) — nenhum evento novo é proposto aqui.

## Restrições herdadas do `ANALYTICS-PLAN.md` (não repetir aqui, só lembrar)

- Firebase Spark (free): sem Cloud Functions, sem BigQuery, sem pipeline própria. Tudo abaixo é
  observável só via **Firebase console → Analytics → Events/Explore (Funnel/Retention)** e
  **Crashlytics** — painéis manuais, não um dashboard automatizado.
- Latência de 24–48h fora do DebugView. DebugView só serve para verificar em tempo real que um
  evento dispara durante QA com um device próprio — não serve para observar o cohort externo de
  beta testers.
- `AnalyticsTracker.setUserId` não está plugado em lugar nenhum — não há como reconstruir a jornada
  de um tester entre reinstalação/troca de aparelho. Os KPIs abaixo são todos por evento/contagem
  agregada, não por jornada individual rastreada.
- Todos os cálculos de razão (ex.: completados / iniciados) são feitos **manualmente**, lendo as
  contagens brutas do relatório de Events e dividindo numa planilha — não há um funil automático
  calculando a porcentagem para nós além do módulo Explore (que é gratuito e deve ser usado quando
  disponível, mas os números abaixo também precisam bater fazendo a conta à mão como checagem).

## Janela de observação e tamanho de cohort (premissa explícita)

Nenhum dos três documentos-fonte define um número fixo de testers do primeiro grupo fechado. Assumindo
um primeiro grupo pequeno e de confiança (ordem de grandeza de 15–50 pessoas, típico de uma trilha
"Internal testing"/"Closed testing" inicial no Play Console — ver `BETA-STORE-READINESS.md`), os KPIs
abaixo devem ser lidos sobre uma **janela mínima de 14 dias corridos** após os primeiros testers
entrarem, revisada semanalmente. Isso importa para a leitura dos números: com N pequeno, um único
tester confuso ou desistente já move vários pontos percentuais — por isso os pisos abaixo são
deliberadamente conservadores (não são metas de produto maduro, são o mínimo para não soar alarme).
Se em 14 dias o cohort ativo (>= 1 `app_open`) for menor que ~10 pessoas, adie a leitura dos KPIs —
a amostra não é interpretável ainda, e isso por si só deve ser reportado ao `pm` (baixa adesão ao
convite, não um problema do produto necessariamente).

---

## KPIs bloqueantes (gate para seguir ao lançamento público)

Cada linha abaixo tem: o funil (nº do `ANALYTICS-PLAN.md`), o piso mínimo, por que é piso e não teto,
e o que fazer se ficar abaixo.

### 1. Ativação — completar o loop core na primeira sessão

- **Funil 1** (`app_open` → `shopping_list` screen_view → `shopping_list_created` →
  `product_added`/`product_selected`).
- **Piso mínimo:** >= 60% dos testers que abrem o app pela primeira vez criam uma lista **e**
  adicionam pelo menos 1 produto na mesma sessão.
- **Por que é piso, não teto:** 60%, não 90%, porque parte do cohort de um beta fechado
  costuma abrir o app só para "dar uma olhada" e não converte de propósito — isso é esperado e
  aceitável num primeiro contato. O que não é aceitável é que a maioria trave: abaixo de 60% é sinal
  de que o próprio caminho feliz (criar lista → adicionar produto), a proposta de valor central do
  app, está confuso ou quebrado para quem acabou de instalar — e nenhuma outra métrica abaixo importa
  se essa etapa não funciona.
- **Se abaixo do piso:** não avançar para beta aberto/público; investigar no funil onde o drop-off
  concentra (tela `shopping_list` nunca vista? lista criada mas nenhum produto adicionado?) antes de
  convidar mais gente.

### 2. Conclusão de compra — a sheet de finalizar não pode ser o funil que trava

- **Funil 2**: razão `cart_finish_purchase_completed` / `cart_finish_purchase_started`.
- **Piso mínimo:** >= 50%.
- **Por que é piso, não teto:** testers abrem a sheet de finalizar compra por curiosidade sem ter
  de fato terminado as compras físicas — abandono nessa etapa é parcialmente esperado por não ser um
  fluxo forçado a terminar na mesma sessão. Abaixo de 50%, porém, deixa de ser "abandono por não ter
  terminado de comprar ainda" e passa a ser mais provável um problema de UI/validação na própria
  sheet (o valor gasto travando salvar, por exemplo) — o tipo de bug que faria um cliente real achar
  que perdeu dados.
- **Se abaixo do piso:** tratar como bug de UX na sheet de finalizadvel purchase antes de ampliar o
  cohort; `amount` do evento completado serve de checagem adicional — valores muito fora do plausível
  sugerem testers "brincando" com o app, não usando de verdade, e isso deve ser considerado ao ler o
  número junto com o `pm`.

### 3. Colaboração via lista compartilhada — o diferencial do app precisa funcionar

- **Funil 3**: razão `shopping_list_joined` / (`shopping_list_joined` + `shopping_list_join_failed`),
  quebrado por `join_method`.
- **Piso mínimo:** >= 70% de sucesso agregado, e nenhum `join_method` individual abaixo de 50%.
- **Por que é piso, não teto:** compartilhar/entrar em lista via token ou QR é a feature-headline do
  app (o motivo pelo qual Lucas e Bianca-e-Diego, personas do `pm`, usariam o Cestou em vez de uma
  lista qualquer) — não é um recurso periférico onde falha ocasional é tolerável. 70%, não 100%,
  porque tokens digitados errado por engano acontecem mesmo em produtos maduros; abaixo disso, porém,
  o diferencial do produto não é confiável o bastante para recomendar a um amigo — que é literalmente
  o próprio mecanismo de compartilhamento sendo testado.
- **Dependência conhecida:** este KPI só deve ser lido com confiança depois que **G13** (scanner de
  QR sem debounce, spamma notificação de entrada duplicada — ver `MVP-ROADMAP.md`) estiver corrigido
  e mesclado. Enquanto G13 estiver aberto, uma taxa de sucesso de join aparentemente normal pode
  esconder entradas duplicadas silenciosas (o join "funciona", mas dispara notificação repetida para
  os outros membros) — o que este KPI, por si só, não captura. Reportar isso como limitação ao `pm`
  junto com o número, não maquiar o piso como "atingido" se G13 ainda estiver aberto.
- **Se abaixo do piso:** se `join_method = qr_scan` for o que puxa a média pra baixo, aponta bug de
  câmera/geração de QR, não de backend (conforme já observado no `ANALYTICS-PLAN.md`).

### 4. Retenção curta — D7

- **Fonte:** relatório nativo de retenção do GA4/Firebase (Analytics → Explore → Retention), que usa
  `app_open` (evento que já existe) — não precisa de instrumentação nova.
- **Piso mínimo:** >= 25% dos testers retornam ao app dentro de 7 dias do primeiro `app_open`.
- **Por que é piso, não teto:** 25% é uma referência de piso comum para apps utilitários de uso
  recorrente não-diário (o Cestou não é um app para abrir todo dia, é para abrir quando alguém vai às
  compras) — mesmo um cohort favorável (amigos/família dispostos a ajudar) não deveria inflar isso
  artificialmente muito além disso numa primeira semana. Abaixo de 25%, o sinal é que o app não está
  virando hábito nem para o público mais complacente que existe (o próprio grupo de beta testers
  convidados de propósito) — o que é um alarme mais sério do que parece, porque esse cohort é
  estruturalmente mais tolerante que o público real do lançamento.
- **Se abaixo do piso:** investigar junto ao `pm` se é abandono após primeira lista finalizada (falta
  de motivo para reabrir) ou fricção logo na primeira sessão (ligar com o KPI de ativação acima).

### 5. Saúde técnica — estabilidade (Crashlytics)

- **Piso mínimo:** crash-free users rate >= 97%, em janela rolante de 7 dias.
- **Por que é piso, não teto:** 97%, não 99,5%, porque **F0.3** (suíte Maestro) e **F2.2** (QA de
  Google Sign-In num device real) nunca rodaram de fato neste app — o app está entrando no beta sem
  ter sido exercitado ponta a ponta num device real nenhuma vez antes. Algum ruído de crash inicial
  em combinações de device/Android não testadas é esperado. Abaixo de 97%, porém, o problema deixa de
  ser "device raro não coberto" e passa a ser instabilidade sistêmica — inaceitável para convidar mais
  gente.
- **Se abaixo do piso:** pausar convites de novos testers até identificar e corrigir a causa raiz via
  Crashlytics; não é um item para "monitorar e seguir em frente".

### 6. Saúde técnica — fricção de login

- **Evento:** `auth_sign_in_failed` (`WelcomeViewModel`), quebrado por `reason`.
- **Limitação de instrumentação:** não existe um evento `auth_sign_in_attempted`/contagem de
  tentativas no inventário atual — não dá para calcular uma taxa limpa de "falhas / tentativas". O
  KPI abaixo usa a métrica que a instrumentação atual sustenta: proporção de usuários únicos que
  registram pelo menos 1 `auth_sign_in_failed`, sobre o total de usuários únicos que chegam à tela
  `welcome` (screen_view automático do SDK do Firebase, ligado por padrão, sem precisar de
  instrumentação manual).
- **Piso mínimo:** < 20% dos usuários únicos que chegam à tela `welcome` registram pelo menos uma
  falha de login.
- **Por que é piso, não teto:** o fluxo real de Google Sign-In (SHA-1, tela de consentimento OAuth)
  nunca rodou ao vivo neste app (F2.2 ainda em aberto) — algum atrito inicial de configuração é
  esperado antes de ser corrigido. Login, porém, é a porta de entrada literal do app: acima de 20% de
  falha visível já é sinal de que uma fatia grande do cohort (a persona Dona Célia, zero paciência
  para fricção, é o caso extremo citado pelo `pm`) provavelmente desiste sem nem tentar de novo — o
  que também contamina a leitura de todos os outros KPIs acima, porque o denominador de quem "chegou
  a usar o app de verdade" já estaria artificialmente reduzido.
- **Se abaixo do piso:** tratar F2.2 como bloqueante e urgente, não "ainda não verificado" — é
  literalmente o que este próprio KPI já teria detectado indiretamente.

### 7. Canário de confiabilidade da própria telemetria

- **Sinal:** qualquer ocorrência de `AnalyticsException` no Crashlytics (try/catch do
  `FirebaseAnalyticsTracker`, conforme documentado no `ANALYTICS-PLAN.md`).
- **Piso mínimo:** 0 ocorrências na janela de observação.
- **Por que é piso e não teto (na verdade é um piso absoluto, não percentual):** se esse evento
  aparecer, significa que chamadas de `trackEvent` estão falhando silenciosamente — ou seja, todos os
  KPIs acima (1–6) podem estar subcontando sem que ninguém perceba pelo número em si. Não é um KPI de
  produto, é uma checagem de que os outros KPIs são confiáveis o bastante para decidir algo em cima
  deles.
- **Se aparecer:** tratar os números dos KPIs 1–6 coletados na mesma janela como suspeitos até
  investigar a causa — não descartar automaticamente, mas não usar como único critério de decisão
  nesse período.

---

## Métricas de diagnóstico (não bloqueiam o go/no-go, mas devem ser reportadas)

Estas vêm diretamente dos funis 4 e 6 do `ANALYTICS-PLAN.md`, que o próprio documento já enquadra como
insumo de priorização de produto, não como sinal de usabilidade quebrada — reproduzindo esse
enquadramento aqui de propósito, para não inventar um piso que a instrumentação/racional não sustenta:

- **Funil 4 — como os produtos são adicionados** (`product_selected` vs. `product_added` por
  `source`: `recipe`, `common_product`, `quick_add`, `photo_scan`). Sem piso mínimo: é descritivo. Se
  algum `source` nunca disparar, é um insumo para o `pm` decidir prioridade pós-beta daquela entrada
  — não motivo para segurar o lançamento, já que as outras entradas (busca, digitação rápida) cobrem
  o caso de uso básico de adicionar produto de qualquer forma.
- **Funil 5 — confiabilidade do scan por foto com IA** (`product_photo_scan_performed`,
  distribuição de `result_count`, especialmente `result_count = 0`, vs.
  `product_photo_scan_failed`). Sugestão de limiar de atenção (não bloqueante): se
  `result_count = 0` + falhas juntos passarem de 40% das tentativas, reportar ao `pm` como candidato
  a receber um nudge de "adicionar manualmente" na UI — mas não travar o beta por isso, porque a
  busca/adição manual já existe como caminho alternativo funcional para o mesmo objetivo (adicionar
  produto à lista).
- **Funil 6 — adoção de limite de gastos** (`shopping_budget_set` com `has_budget=true`, sobre o
  total de `shopping_list_created`). Sem piso mínimo: informa se o `pm` deve investir mais ou menos
  em polish dessa feature no roadmap pós-beta, conforme o próprio `ANALYTICS-PLAN.md` já enquadra.

## Regra de decisão (go / no-go para lançamento público)

O beta é considerado saudável o suficiente para seguir ao lançamento público quando, **na mesma
janela de observação de pelo menos 14 dias**, todos os itens abaixo forem verdadeiros ao mesmo tempo:

1. Os 7 KPIs bloqueantes acima (seções 1–7) atingem o piso mínimo.
2. Nenhuma dependência conhecida está pendente sem nota — em particular, se G13 (item 3) ou F2.2
   (item 6) ainda estiverem em aberto quando os números forem lidos, isso deve ser reportado junto
   com o número, não silenciado, mesmo que o número em si pareça bom.
3. **O checklist de prontidão do `pm`** ("Definition of ready for beta", Fase 1 do roadmap fechada —
   ver `.specs/BETA-LAUNCH-PLAN.md`) está confirmado fechado, incluindo a instrumentação dos fluxos-
   chave já estar visível no DebugView. Este item (3) é uma condição **adicional** aos KPIs acima, não
   substituível por eles: números bons de um cohort pequeno não compensam itens de segurança/compliance
   (G3, G5) ou QA de device (F0.3, F2.2) ainda não confirmados — essa é a mesma amarração que o `pm` já
   estabeleceu na seção "Beta Launch Priority" do `MVP-ROADMAP.md`.

**Este documento não anuncia data nenhuma.** A decisão de anunciar o beta como "pronto" ou de avançar
para lançamento público é do `pm` em conjunto com bruno, cruzando estes números com o checklist citado
no item 3 acima — papel do `marketing` aqui é só definir o piso mensurável, não decidir o timing.
