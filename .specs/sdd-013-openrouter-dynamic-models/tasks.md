# Task: Carregar modelos de IA dinamicamente da API do OpenRouter

- **Slug:** openrouter-dynamic-models
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-28
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- O app possui atualmente uma lista fixa de modelos de IA (`AiModel.freeModels` em `core/domain/.../AiModel.kt`).
- O usuário solicitou que a lista de modelos use a API do OpenRouter (`https://openrouter.ai/api/v1/models`) como base **sem** fallback para lista estática.
- O valor padrão vem do Remote Config (`BuildConfig.OPEN_ROUTER_MODEL`), mas sempre que possível, o app prioriza o primeiro modelo gratuito (`free`) da lista dinâmica.
- Rastreabilidade: `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/AiModel.kt:13-31`, `feature/settings/src/main/java/br/com/brunocarvalhs/howmuch/feature/settings/presentation/screen/AiSettingsScreen.kt:70-115`.

## 2. Escopo

**Inclui:**
- Criação/extensão de serviço de rede (via Ktor `HttpClient`) para consultar a rota `https://openrouter.ai/api/v1/models`.
- Parsing do JSON de resposta da API do OpenRouter para mapear para `AiModel` (identificando ID, nome, vendor/criador, e se é gratuito com base em pricing ou sufixo `:free`).
- Camada de domínio/repositório para fornecer a lista dinâmica de modelos (sem fallback estático).
- Seleção de modelo padrão: Remote Config (`BuildConfig.OPEN_ROUTER_MODEL`), priorizando o primeiro modelo gratuito (`free`) da lista dinâmica quando aplicável.
- Atualização em `SettingsViewModel` e `AiSettingsScreen` para observar/carregar a lista dinâmica de modelos.
- Testes unitários para o parser de modelos e repositório dinâmico.

**Exclui:**
- Fallback para lista estática (`AiModel.freeModels`).

## 3. Impacto Arquitetural

- `:core:ai` ou `:core:data` / `:core:domain`: repositório/fontes de dados para listagem dinâmica de modelos.
- `:feature:settings`: `SettingsViewModel` e `AiSettingsScreen` atualizados.

Diagrama de fluxo:
```
OpenRouter API (`https://openrouter.ai/api/v1/models`)
  └── HttpClient (Ktor)
       └── AiModelRepository / Service (Sem fallback estático)
            └── SettingsViewModel (Default via Remote Config / 1º free dinâmico) -> AiSettingsScreen
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Justificativa |
|---|---|---|---|
| `AiModelRepository.getAvailableModels(): List<AiModel>` | Novo método ou expansão | `public` / `internal` | Fornecer modelos dinâmicos |
| `OpenRouterModelResponse` | Novos DTOs de deserialização | `internal` | Mapear JSON da API |

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Lista estática `AiModel.freeModels` | Lista 100% dinâmica via OpenRouter API `/api/v1/models` (sem fallback estático) | `AiModel.kt:23-31` |
| 2 | Padrão estático | Padrão via Remote Config, priorizando o primeiro modelo gratuito (`free`) da lista dinâmica | `SettingsRepositoryImpl.kt:53` |

## 6. Critérios de Aceitação
- Consulta bem-sucedida a `https://openrouter.ai/api/v1/models` retorna a lista dinâmica de modelos.
- Ausência total de fallback para lista estática.
- Modelo padrão obtido via Remote Config ou o primeiro modelo `free` da lista dinâmica.
- Dropdown em `AiSettingsScreen` exibe os modelos dinâmicos.

## 7. Plano de Testes
- **Unitários:**
  - Parsing do JSON da API do OpenRouter.
  - Seleção do primeiro modelo `free` dinâmico.
- **Lint/Detekt/Testes:**
  - `./gradlew lintDebug detekt testDebug`

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Indisponibilidade da API do OpenRouter | baixa | médio | Tratamento de erro adequado na UI sem quebrar o app |

## 9. Perguntas em Aberto (bloqueantes)
Nenhuma.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
- [x] Seção 6 preenchida.
- [x] Perguntas em aberto exauridas.
