# Task: Compartilhar 1 lista no plano gratuito

- **Slug:** compartilhamento-gratuito-uma-lista
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-16
- **Status:** draft
- **Versão-alvo:** a definir
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive
- **Prioridade:** depois de sdd-061 a sdd-064

## 1. Contexto e Motivação
- Compartilhar a lista (QR e convite) é PRO sem teste (AD-010). É o laço que traz um segundo usuário da família, e o principal motivo de uso recorrente nos concorrentes (Bring!, Listonic).
- Decisão do Bruno em 16/11/2026: o gratuito compartilha **1 lista**; mais listas compartilhadas continuam PRO.
- Rastreabilidade: `feature/shopping/.../navigation/ShoppingGraph.kt` (gate do QR), `feature/cart/.../navigation/CartGraph.kt` (gate de compartilhar).

## 2. Escopo
- **Inclui:**
  - Usuário FREE compartilha a primeira lista sem paywall; a segunda abre o paywall com origem `share_limit`.
  - Contagem de listas compartilhadas = listas onde o usuário é `OWNER` e há mais de um membro.
  - Texto no paywall explicando o limite.
- **Exclui:**
  - Mudança no papel `EDITOR` de quem entra na lista (entrar continua livre).

## 3. Impacto Arquitetural
- Módulos afetados: `feature:shopping`, `feature:cart`, `core:domain` (use case `CanShareShoppingUseCase`).
- AD-010 continua valendo: a decisão de PRO segue em `SubscriptionRepository.observeStatus()`; o limite é uma regra nova ao lado dele, registrada como AD nova.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Compartilhar exige PRO | FREE compartilha 1 lista; PRO sem limite | `ShoppingGraph.kt`, `CartGraph.kt` (`if (isPro)`) |

## 6. Critérios de Aceitação
- Eventos `share_limit_reached` e `paywall_viewed` com `source = share_limit`.
- Meta: listas com 2 ou mais membros e retenção de semana 1 maior nessas contas.

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Se o PRO expira com 3 listas compartilhadas, as listas continuam compartilhadas? (sugestão: sim, só bloqueia novas)

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
