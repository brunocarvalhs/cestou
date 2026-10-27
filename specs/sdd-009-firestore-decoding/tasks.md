# Task: Decodificação robusta de Firestore e limpeza de histórico

- **Slug:** firestore-decoding
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Um único documento malformado esconde todas as listas ou produtos: decodificação é tudo-ou-nada (B-21). O snapshot listener fecha de vez em erro, emitindo lista vazia (B-22). "Criptografia" (Base64URL) decodifica qualquer string que seja valid Base64 e volte idêntica (B-23), corrompendo dados silenciosamente. Além disso, histórico de produto cresce sem limite, risco ao limite de 1 MB de documento (F-10). Este SDD oferece decodificação resiliente elemento-a-elemento, retry com backoff, versionamento seguro e limite de histórico.

**Itens do backlog:** B-21, B-22, B-23, F-10 (partialmente, mudança de modelo é em sdd-010).

## 2. Escopo

**Inclui:**
- Decodificar Firestore `List<T>` elemento a elemento, descartar e registrar inválidos.
- `retryWhen` com backoff exponencial no snapshot listener.
- Expor estado de erro no Flow (não lista vazia), para UI mostrar "Nenhuma lista" vs "Erro ao carregar".
- Marcar valores legados com prefixo de versão (`_v1_`) ou migração de Base64.
- Limitar histórico de produto a N últimas entradas (proposta: 20).
- Remover campos `position` muito antigos (>6 meses) do histórico.

**Exclui:**
- Migração offline (fica para 2.3.0).
- UI de erro (fica para sdd-002, F-01).
- Teste de Firestore rules (fica para D-01 publication).

## 3. Impacto Arquitetural

```
:core:data/network
  ├── NetworkServiceImpl.kt
  │   ├── deserialize element-a-element [mudança]
  │   └── log de inválidos
  └── FirestoreClient.kt
      ├── retryWhen com backoff [novo]
      └── estado de erro no Flow [novo tipo]
      
:core:domain/model
  └── Product.kt
      ├── history: List<Activity> [limite novo]
      └── companion object para limite (const)
      
:feature:shopping/data/repository
  └── ShoppingRepositoryImpl.kt
      ├── expor `fetchAllError: StateFlow<Throwable?>`  [novo]
      └── UI lê e mostra
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `NetworkServiceImpl.deserialize<List<T>>` | Elemento-a-elemento | `internal` | log de falha |
| `ShoppingRepositoryImpl.fetchAllError` | Novo | `public` (StateFlow) | UI mostra erro |
| `Product.history` | Limite N | `public` | compatível |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Um doc inválido derruba tudo | Descartar inválido, emitir válidos + log | B-21 |
| 2 | Erro transiente fecha listener | Retry com backoff, estado de erro na UI | B-22 |
| 3 | Base64 corrompe dados silenciosamente | Versão prefixada (`_v1_`), ou migração | B-23 |
| 4 | Histórico cresce sem limite | Máximo de 20 entradas (ou N) | F-10 |

## 6. Critérios de Aceitação

- **Decodificação elemento-a-elemento:** JSON inválido em um item → descartado, outros processados. Exemplo: `[{valid}, {invalid}, {valid}]` → emite 2 itens.
- **Retry backoff:** `FlowCollector<List<T>>` com `retryWhen { cause, attempt → delay(2^attempt) }`, máximo de 5 tentativas.
- **Erro exposto:** `ShoppingRepositoryImpl.fetchAllError: StateFlow<Throwable?>`. UI: se erro, exibe "Erro ao carregar. Tente novamente."; se sucesso, exibe listas.
- **Base64 versionado:** novo valor salvo tem prefixo `_v1_encrypted_`. Decodificação tenta prefixo, fallback a Base64 direto (compatível).
- **Histórico limitado:** ao adicionar entrada, manter só últimas 20. Ao desserializar, truncar se >20.
- **Migração suave:** documentos antigos sem limite funcionam até serem reescritos.

## 7. Plano de Testes

**Unitários:**
- B-21: `[{id:"1"}, {x:"invalid"}, {id:"2"}]` → desserializa 2 itens, loga erro do segundo.
- B-22: Listener falha com `PermissionDeniedException` → retry, depois erro emitido em `fetchAllError`.
- B-23: Salvar `"ABC"` com versão nova, ler → não descodifica. Ler valor antigo sem versão → tenta Base64.
- F-10: Produto com 30 entradas no histórico → deserializa/exibe 20. Adicionar 1 → mantém 20.

**Instrumentados:**
- Cortar internet → app mostra "Erro ao carregar" (não "Nenhuma lista"). Reconectar → retenta.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Descartar docs inválidos perde dados | baixa | médio | Log detalhado + monitorar em Crashlytics |
| Retry backoff não resolve permissão | baixa | baixo | Usuário vê erro, pode fazer logout e tentar novamente |
| Versionamento Base64 complica código | baixa | baixo | Docstringcomenta a transição |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (B-21, B-22, B-23, F-10).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] Compatibilidade com dados legados.
- [x] Testes instrumentados planejados.
