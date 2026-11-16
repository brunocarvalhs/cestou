# Task: Lembrete de compra ativo por padrão e que abre a lista

- **Slug:** lembrete-de-compra-ativo
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-16
- **Status:** ready
- **Versão-alvo:** próxima minor
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive
- **Prioridade:** 1 de 4 (retenção)

## 1. Contexto e Motivação
- Retenção de semana 1 de 3,7% e ~73% de desinstalação na janela de 17/10 a 13/11/2026 (Firebase). O app não tem nenhum gatilho ativo para trazer o usuário de volta.
- O lembrete de lista pendente já existe, mas nasce desligado (`remindersEnabled = false`) e a notificação não abre nada ao ser tocada.
- Referência: auditoria de experiência e retenção (16/11/2026), achado "Lembretes desligados por padrão".
- Rastreabilidade: `core/domain/.../model/AppSettings.kt:17`, `feature/settings/.../worker/ShoppingReminderWorker.kt:52,93`.

## 2. Escopo
- **Inclui:**
  - `remindersEnabled` passa a `true` por padrão para instalações novas.
  - A notificação abre a lista pendente mais recente (uma lista) ou a home (várias), via `PendingIntent`.
  - O lembrete só dispara se houver lista não finalizada e se o app não foi aberto no mesmo dia (dia lido do `AppUsageTracker`).
  - O agendamento roda ao entrar no app e após a permissão de notificação, não só ao salvar configurações.
  - Evento `reminder_shown` (parâmetro `pending_count`).
- **Exclui:**
  - Mudar o valor salvo de quem já desligou o lembrete.
  - Lembrete por dia da semana habitual (escopo de sdd-063).
  - Evento `reminder_opened` e toggle no Remote Config (próxima fatia).

## 3. Impacto Arquitetural
- Módulos afetados: `core:domain`, `feature:settings`, `core:analytics`, `app` (deep link de entrada).
- Padrão aplicado: Clean Architecture + MVI; o worker continua usando `EntryPoints`.

## 4. Contratos e Interfaces
- Nenhum contrato público novo. Novas constantes em `AnalyticsEvents`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Lembrete desligado por padrão | Ligado por padrão em instalação nova | `core/domain/.../model/AppSettings.kt:17` |
| 2 | Notifica se houver qualquer lista não finalizada | Notifica só se houver lista não finalizada e o app não foi aberto hoje | `feature/settings/.../worker/ShoppingReminderWorker.kt:52` |
| 3 | Tocar na notificação não abre o app | Abre o app na tela de listas |
| 4 | Agendamento só ao salvar configurações | Agendamento também ao entrar no app | `core/data/.../repository/SettingsRepositoryImpl.kt:142` | `feature/settings/.../worker/ShoppingReminderWorker.kt:93` |

## 6. Critérios de Aceitação
- Instalação nova recebe o lembrete às 18:00 sem passar pelas configurações, desde que a permissão de notificação tenha sido dada.
- No máximo uma notificação por dia; nenhuma no dia em que o app já foi aberto.
- Feature toggle (próxima fatia): `feat_reminder_default_on` (Remote Config, default `true`), para desligar sem nova versão.
- Meta: retenção de semana 1 de 3,7% para 6% nas coortes da nova versão.

## 7. Plano de Testes
- **Unitários:** decisão de disparo do worker (sem lista, lista aberta, app aberto hoje, dia desconhecido).
- **Unitários:** `syncReminders` agenda o lembrete quando nada foi salvo.
- **Manual:** tocar na notificação abre o app.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Notificação diária vira motivo de desinstalação | Média | Alta | Regra de "app não aberto hoje", toggle remoto e acompanhar `app_remove` por versão |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
