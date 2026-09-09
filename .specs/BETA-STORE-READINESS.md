# Beta / Store Readiness — Cestou (How-Much)

Status: Draft — checklist da trilha de teste, release notes e canal de feedback prontos para revisão.
Owner: `marketing`
Last updated: 2026-09-09

Executa a parte "Beta/store readiness" atribuída ao `marketing` em `.specs/BETA-LAUNCH-PLAN.md`. Não
edita `BETA-LAUNCH-PLAN.md`, `MVP-ROADMAP.md` nem `BETA-KPI.md` (arquivos de outros donos). Usa o que
já existe: `fastlane/metadata/android/` (descrições de loja, PR #22) e `docs/legal/` (páginas de
Privacidade/Termos, PR #21).

## O que este documento NÃO decide (fora do escopo do `marketing`)

- **Hospedagem das páginas legais (G3)** — `cestou.app` vs. GitHub Pages. Decisão do bruno. Este
  documento assume que a decisão ainda não foi tomada e trata isso como bloqueio explícito abaixo.
- **Upload/publicação real no Play Console** — quem publica é o dono do produto. Este documento
  entrega o checklist e os textos prontos, não executa nenhum passo dentro do Console.
- **Código Kotlin do app** — nenhuma mudança de código está incluída aqui.

---

## 1. Checklist da trilha de teste fechado/interno no Play Console

### 1.1 Escolha de trilha

Recomendação: começar em **Internal testing** (até 100 testadores, sem necessidade de revisão do
Google, ativa em minutos), não em **Closed testing** diretamente — permite validar o processo de
publicação e o primeiro convite/opt-in com um grupo pequeno (a própria bruno + 1–2 pessoas de
confiança) antes de abrir para o cohort real de beta testers via Closed testing. Motivo prático: F0.3
(Maestro) e F2.2 (Google Sign-In) nunca rodaram num device real — Internal testing é o ambiente mais
barato para fazer essa primeira verificação com device antes de convidar clientes reais.

| Passo | Descrição | Status |
|---|---|---|
| Criar app no Play Console (se ainda não existe) | Nome, pacote, idioma padrão | Não verificável neste ambiente — ação do bruno |
| Selecionar trilha "Internal testing" | Configuração inicial | Ação do bruno |
| Upload do primeiro AAB/APK | Build assinado a partir de `develop` (ou de um branch de release, a decidir por bruno — ver "Release branching" em `BETA-LAUNCH-PLAN.md`) | Bloqueado — decisão de branch de release é do bruno |
| Lista de testadores (Internal testing) | Lista direta de e-mails no próprio Console (não precisa de Google Group para até 100 testadores) | Ação do bruno — sugestão: reaproveitar a mesma lista para promover depois a Closed testing |
| Gerar link de opt-in | Só existe depois de pelo menos 1 release ativo na trilha | Depende do passo anterior |
| Rodar QA no device (F0.3, F2.2) | Via o próprio link de opt-in do Internal testing | Depende de device — sinalizado como bloqueado em `MVP-ROADMAP.md`/`BETA-LAUNCH-PLAN.md` |
| Promover para "Closed testing" com o cohort real | Depois que Internal testing confirmar que o build instala e abre sem quebrar no primeiro contato | Ação do bruno, após o passo de QA acima |

### 1.2 Ficha da loja (Store listing) — o que falta para a trilha aceitar publicação

Mesmo trilhas de teste (fechado/interno) exigem uma ficha mínima de loja para publicar. Estado atual:

| Item | Status | Bloqueio |
|---|---|---|
| Descrição curta/completa (en/pt-BR/es-ES) | Pronto — `fastlane/metadata/android/{en-US,pt-BR,es-ES}/` (PR #22) | Nenhum |
| Ícone do app | Presumivelmente já existe no projeto (não verificado por este agente — fora do escopo revisar assets binários de app) | Confirmar com `tech-lead`/bruno |
| Screenshots (mín. 2, recomendado 4–8) | **Faltando** | Precisa de device/emulador — sinalizado, não posso gerar |
| Feature graphic (1024×500) | **Faltando** | Precisa de device/emulador (ou pelo menos uma ferramenta de design) — sinalizado, não posso gerar |
| Categoria do app / classificação de conteúdo (questionário) | Não preenchido | Ação do bruno no Console |
| Data safety form (declaração de dados coletados) | Não preenchido — ver rascunho de referência abaixo | Ação do bruno no Console, mas o rascunho abaixo acelera o preenchimento |
| URL de Política de Privacidade | **Bloqueado** | Depende de G3 (hospedagem) — ver seção 1.4 |

**Rascunho de referência para o Data Safety form** (baseado no que o código realmente faz, não no que
poderia fazer — cruzar com `tech-lead` antes de preencher no Console):
- Coleta de dados de conta: e-mail e nome (Firebase Auth / Google Sign-In), usado para
  autenticação e identificação dentro de listas compartilhadas.
- Dados de app: listas de compras, produtos, orçamento — armazenados no Firestore, associados à
  conta do usuário.
- Analytics: eventos de uso (Firebase Analytics) e relatórios de falha (Crashlytics) — conforme
  `.specs/ANALYTICS-PLAN.md`, sem PII nos parâmetros dos eventos custom.
- Sem anúncios, sem venda de dados a terceiros (nenhuma integração de ads no projeto).
- O app funciona sem conta para uso local; conta é necessária só para sincronizar entre aparelhos ou
  compartilhar listas (conforme a própria descrição de loja já publicada).

### 1.3 Aviso de que é uma versão beta

Play Console já mostra automaticamente um aviso genérico de "app de teste" para quem entra via link de
opt-in de Internal/Closed testing — isso cobre o requisito mínimo sem trabalho extra. Complementar com
um texto próprio no e-mail/mensagem de convite (não é uma tela dentro do app — isso seria mudança de
código, fora de escopo):

> **Texto sugerido para o convite (pt-BR):**
> "Você está entrando na versão beta fechada do Cestou. É um app real, funcional, mas ainda em teste —
> pode ter bugs, comportamentos inesperados ou pequenas instabilidades. Se algo quebrar ou parecer
> estranho, é exatamente isso que queremos que você reporte — veja como no final desta mensagem.
> Seus dados de teste (listas, produtos) são reais e ficam salvos normalmente, mas por ser uma fase
> inicial, recomendamos não depender 100% do app ainda para compras críticas. Obrigado por ajudar a
> deixar o Cestou melhor antes do lançamento para todo mundo!"

### 1.4 Bloqueio a sinalizar para `tech-lead`/`pm` — hospedagem das páginas legais (G3)

`docs/legal/privacy.html` e `docs/legal/terms.html` já estão redigidas (PR #21), mas sem URL pública.
Isso bloqueia:
1. O campo "Privacy Policy URL" no Play Console (obrigatório para publicar, mesmo em teste fechado).
2. O link dentro do app em `CustomMethodPickerTerms` e em Settings — hoje não levam a lugar nenhum.

Isso não é decidido por este agente. Repetindo aqui, como pedido, o lembrete explícito: assim que
bruno decidir entre `cestou.app` e GitHub Pages, o `tech-lead`/`pm` precisa garantir que a URL seja
plugada nos três lugares acima antes de qualquer submissão ao Play Console — não só na loja, também no
app em si.

---

## 2. Release notes (pt-BR primeiro, en/es se der tempo)

Baseadas só no que o app **realmente entrega hoje** (README + descrições de loja já publicadas) — sem
prometer nada de G9–G16 que ainda não está fechado (reordenar lista por arrastar depende do PR #67
ainda não mesclado; o scanner de QR ainda não tem o fix de debounce mesclado — G13 aberto — por isso
release notes não afirmam "compartilhamento sem duplicidade" ou fazem qualquer promessa de
confiabilidade nesses dois pontos específicos).

### pt-BR (idioma principal)

```
Cestou — Versão Beta 1.3.0

Bem-vindo(a) à primeira versão beta do Cestou! Esta é uma versão real e funcional, mas ainda em
fase de testes — sua ajuda encontrando problemas é o que torna o lançamento público melhor.

O que você já pode fazer:
• Criar e organizar quantas listas de compras quiser, cada uma com seu próprio orçamento
• Adicionar produtos buscando pelo nome, digitando rápido, escolhendo dos itens mais comuns, ou
  tirando uma foto e deixando a IA identificar os produtos pra você
• Conversar com o assistente de IA para montar ou revisar sua lista
• Compartilhar uma lista com outras pessoas usando um código ou QR Code
• Definir um limite de gastos por lista e acompanhar o total em tempo real
• Rever o histórico de compras depois que uma lista é finalizada
• Escolher tema (claro/escuro/automático), idioma (pt/en/es) e moeda
• Gerenciar seus dados: limpar cache, apagar dados locais ou excluir sua conta, direto nas
  Configurações

Por ser uma versão beta:
• Pode haver bugs ou comportamentos inesperados — é esperado, e é exatamente isso que queremos que
  você reporte
• Algumas telas e fluxos ainda estão sendo ajustados com base no seu feedback

Encontrou algo estranho? Conte pra gente — o link para reportar está na mensagem de convite do beta
e também nas Configurações do app.

Obrigado por fazer parte disso!
```

### en-US (se houver tempo/necessidade)

```
Cestou — Beta Version 1.3.0

Welcome to Cestou's first beta! This is a real, working build — still in testing, and your help
finding issues is what makes the public launch better.

What you can already do:
• Create and organize as many shopping lists as you want, each with its own budget
• Add products by searching, quick typing, picking from common items, or taking a photo and
  letting AI identify the products for you
• Chat with the AI assistant to build or review your list
• Share a list with other people using a code or QR code
• Set a spending limit per list and track the total in real time
• Review your purchase history after a list is finished
• Choose theme (light/dark/auto), language (pt/en/es), and currency
• Manage your data: clear cache, delete local data, or delete your account, right from Settings

Because this is a beta:
• Expect occasional bugs or unexpected behavior — that's exactly what we want you to report
• Some screens and flows are still being adjusted based on your feedback

Found something odd? Let us know — the link to report is in your beta invite and also in the app's
Settings.

Thanks for being part of this!
```

### es-ES (se houver tempo/necessidade)

```
Cestou — Versión Beta 1.3.0

¡Bienvenido/a a la primera versión beta de Cestou! Esta es una versión real y funcional, todavía
en fase de pruebas — tu ayuda encontrando problemas es lo que hace mejor el lanzamiento público.

Lo que ya puedes hacer:
• Crear y organizar tantas listas de compras como quieras, cada una con su propio presupuesto
• Añadir productos buscando por nombre, escribiendo rápido, eligiendo entre los artículos más
  comunes, o tomando una foto y dejando que la IA identifique los productos por ti
• Conversar con el asistente de IA para armar o revisar tu lista
• Compartir una lista con otras personas usando un código o código QR
• Definir un límite de gastos por lista y seguir el total en tiempo real
• Revisar el historial de compras después de finalizar una lista
• Elegir tema (claro/oscuro/automático), idioma (pt/en/es) y moneda
• Gestionar tus datos: borrar caché, eliminar datos locales o eliminar tu cuenta, directamente en
  Configuración

Por ser una versión beta:
• Puede haber errores o comportamientos inesperados — es esperado, y es justo lo que queremos que
  reportes
• Algunas pantallas y flujos todavía se están ajustando según tu feedback

¿Encontraste algo raro? Cuéntanos — el enlace para reportar está en la invitación del beta y
también en la Configuración de la app.

¡Gracias por ser parte de esto!
```

---

## 3. Canal de feedback do beta (sem infraestrutura extra, Firebase Spark)

### Recomendação: Google Form como canal primário

Motivo: os beta testers são clientes reais (conforme o enquadramento da tarefa), não necessariamente
pessoas com conta no GitHub ou familiaridade técnica — personas como Dona Célia e Dona Marlene
(citadas em `MVP-ROADMAP.md`) não devem precisar entender o que é uma "issue" para reportar um
problema. Um Google Form:
- Não exige conta além de um e-mail simples.
- É gratuito, sem backend (compatível com a restrição de Spark plan).
- Pode ser linkado diretamente na mensagem de convite do beta (seção 1.3) e num item das
  Configurações do app (mudança de código futura, fora de escopo deste documento — só a sugestão do
  link fica registrada aqui para o `android-engineer-features`/`tech-lead`).

**Estrutura sugerida do formulário (pt-BR, campos):**
1. Seu e-mail (para follow-up, opcional) — texto curto
2. O que você estava tentando fazer? — texto curto
3. O que aconteceu? — parágrafo
4. O que você esperava que acontecesse? — parágrafo
5. Em que tela isso aconteceu? — texto curto (ou múltipla escolha com as telas principais: Lista de
   compras, Adicionar produto, Compartilhar/Entrar em lista, Finalizar compra, Configurações, Outro)
6. Consegue reproduzir de novo? — múltipla escolha (Sempre / Às vezes / Só aconteceu uma vez)
7. Print ou vídeo (opcional) — upload de arquivo
8. Nota geral da experiência até agora (1–5) — escala linear (sinal qualitativo complementar aos KPIs
   de `BETA-KPI.md`, não substitui os eventos de analytics)

A criação do Form em si (Google Forms) é uma ação de configuração fora deste repositório — este
documento entrega a estrutura pronta para bruno criar em poucos minutos quando decidir lançar o beta.

### Canal secundário: template de issue já existente no GitHub

`.github/ISSUE_TEMPLATE/bug.yml` já existe e cobre bem relatos técnicos (passos de reprodução,
ambiente, logcat) — mantê-lo como canal secundário para testadores tecnicamente confortáveis
(ex.: colaboradores do repositório) que preferem reportar direto como issue em vez de preencher um
formulário separado. Não é necessário duplicar esse template para o beta; ele já serve como está.
Se, mais adiante, o volume de feedback de testers não-técnicos via GitHub justificar um template
dedicado em português, isso pode ser um item futuro do `tech-lead` — não criado aqui para não misturar
a estrutura de triagem técnica existente com feedback de usuário final.

---

## 4. Resumo de bloqueios ativos (para `tech-lead`/`pm`/bruno)

| Bloqueio | Depende de | Já sinalizado em |
|---|---|---|
| Screenshots + feature graphic | Device/emulador | `MVP-ROADMAP.md` (G4 remainder), `README.md` |
| URL de Privacidade/Termos | Decisão de hospedagem (bruno) | `MVP-ROADMAP.md` (G3), `BETA-LAUNCH-PLAN.md` |
| Link legal plugado em `CustomMethodPickerTerms`, Settings, Play Console | A URL acima existir | Este documento (lembrete explícito) |
| Build/branch de release para upload no Play Console | Decisão do bruno (`develop` direto vs. merge para `master`) | `BETA-LAUNCH-PLAN.md` ("Release branching") |
| Execução real do checklist do Console (upload, opt-in, Data Safety form) | Ação do bruno | Este documento entrega o conteúdo, não executa |

Nenhum destes bloqueios impede terminar o restante do checklist/textos preparatórios acima — só
impede a publicação final, que continua sendo passo do dono do produto.
