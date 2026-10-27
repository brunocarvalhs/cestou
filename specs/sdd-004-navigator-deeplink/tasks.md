# Task: Navigator singleton e deep linking

- **Slug:** navigator-deeplink
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

`Navigator` é singleton (`@Singleton`) e `MainActivity` usa `launchMode` padrão. Um deep link de convite abre uma segunda instância da Activity, que faz `bind()`. Quando ela fecha, `unbind()` por identidade zera o navigator. A primeira instância fica sem `NavController`, causando `IllegalStateException` ao navegar (C-01). Além disso, o token de convite é processado só no intent de abertura, perdendo-se após login ou recriação de Activity (B-30). Este SDD corrige ambos com `launchMode="singleTask"`, `onNewIntent` e token pendente.

**Itens do backlog:** C-01, B-30.

## 2. Escopo

**Inclui:**
- `launchMode="singleTask"` em `MainActivity` no `AndroidManifest.xml`.
- Implementar `onNewIntent` para processar novos deep links (convite, etc.).
- `SavedStateHandle` para guardar token pendente entre recriações.
- `Navigator.navigate/goBack` ignoram comando se não estiverem vinculados (em vez de lançar exceção), registrando em log.
- Consumir token pendente uma única vez, após autenticação (se necessário).

**Exclui:**
- Migração para `@ActivityRetainedScoped` (fica para refactor futuro).
- Novas rotas ou features de deep link (fica para o produto).

## 3. Impacto Arquitetural

```
:app
  ├── AndroidManifest.xml
  │   └── MainActivity launchMode="singleTask"  [mudança]
  ├── MainActivity.kt
  │   ├── onNewIntent(intent)  [novo]
  │   ├── processDeepLink(intent)  [novo ou refatorado]
  │   └── Navigator.bind/unbind + null checks [mudança]
  └── MainViewModel.kt
      └── pendingJoinToken: SavedStateHandle  [novo]
      
:core:navigation
  └── Navigator.kt
      └── navigate/goBack toleram unbind com log  [mudança]
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `Navigator.navigate(...)` | Tolera unbind, loga em vez de lançar | `public` | compatível com callsites |
| `MainActivity.onNewIntent` | Novo | n/a | processa intent no mesmo contexto |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Uma instância de MainActivity | `singleTask` garante uma só instância; `onNewIntent` processa deep links no mesmo contexto | C-01 |
| 2 | Convite se perde no login | Token armazenado em `SavedStateHandle`, consumido após auth | B-30 |
| 3 | Navegar sem NavController lança exceção | Log de aviso, não lança (não quebra o app) | C-01 |

## 6. Critérios de Aceitação

- **`launchMode="singleTask"`:** uma só instância, `onNewIntent` chamado.
- **`onNewIntent`:** extrai token/rota do intent e notifica ViewModel.
- **`SavedStateHandle`:** token sobrevive recriações (processo restaurado, girar tela).
- **Token consumido uma vez:** depois de ler, apaga do estado (não reprocessado).
- **Navigator seguro:** nenhuma exceção em `navigate/goBack` se unbind.
- **Compatibilidade:** abre app → toca convite em outro app → volta → navega normalmente.

## 7. Plano de Testes

**Unitários:**
- `Navigator.navigate` sem `NavController` vinculado → log, sem exceção.
- `MainViewModel` com token pendente → após login, consome token (ViewModel teste com `SavedStateHandle`).

**Instrumentados:**
- App aberto → abre deep link em outro app (Browser) → volta ao app → navega sem crash.
- Deslogado → toca convite → login → o convite é processado, entra na lista.
- Girar tela com convite pendente → token preservado, processado após auth.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Compatibilidade com intents customizadas | baixa | médio | Testar todas as rotas (convite, reset, etc.) |
| SavedStateHandle indisponível em edge case | baixa | baixo | Fallback a null/ignorar |
| Navigator.bind chamado > 1x em singleTask | baixa | baixo | Idempotência (desbind antes de rebind) |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (C-01, B-30).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] Não quebra contratos existentes.
- [x] Testes instrumentados planejados.
