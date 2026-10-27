---
name: android-architecture
description: Guia de arquitetura do Cestou (Android, Kotlin, multi-módulo) — onde criar arquivos, quais camadas usar, como expor parâmetros públicos e como escolher o módulo certo. Use para dúvidas de estrutura, camadas ou padrões.
---

# Android Architecture (Cestou)

Persona: especialista de arquitetura Android do projeto.

## Fonte de verdade (ordem de prioridade)

1. Código-fonte do projeto.
2. `AGENTS.md`.
3. `.github/copilot-instructions.md`.
4. `README.md` (resumo; não é fonte de verdade).
5. `architecture/01-04` — ainda não existem; quando existirem, consulte-os conforme a pergunta.

## Mapa de módulos

| Módulo | Responsabilidade |
|---|---|
| `:app` | Entrypoint; `FeatureInitializer` orquestra as features. |
| `:core:common` | Utilitários transversais. |
| `:core:domain` | Contratos e modelos compartilhados. |
| `:core:data` | Infraestrutura compartilhada. |
| `:core:theme` / `:core:ui` | Tema Material 3 e componentes reutilizáveis. |
| `:core:navigation` | Base de navegação Nav3. |
| `:core:auth` | Sessão e credenciais. |
| `:core:ai` | Clientes de IA (OpenRouter/Gemini). |
| `:core:remote-config` | Feature toggles e configuração remota. |
| `:core:analytics` | Eventos e telemetria. |
| `:core:billing` | Google Play Billing. |
| `:feature:*` | Features independentes (shopping, products, settings, auth, profile, chat, cart, ai-agent, subscription). |
| `:wear` | Wear OS standalone. |

## Anatomia de uma feature

```
feature/<nome>/src/main/java/br/com/brunocarvalhs/howmuch/feature/<nome>/
├── domain/        modelos, interfaces de repositório, use cases (sem Android)
├── data/          implementações, DTOs, mappers, fontes remota/local
├── presentation/  Composables, ViewModels, estado e intents (MVI)
├── navigation/    NavKey e grafo da feature
└── di/            módulos Hilt
```

## Regras de orientação

- **Onde criar arquivo:** lógica de negócio em `domain/`; acesso a dados em `data/`; estado de tela e Composables em `presentation/`; rota nova em `navigation/`.
- **Dependências:** `presentation` → `domain` ← `data`. `domain` não importa Android nem `data`.
- **Features não dependem umas das outras.** Compartilhamento entre features vai para `:core:*`.
- **Componente reutilizável de UI** vai para `:core:ui`; se for específico de uma feature, fica na feature.
- **Visibilidade:** `internal` por padrão. `public` só para contratos que outro módulo usa, com justificativa.
- **Novo parâmetro público** (modelo, interface, enum, `NavKey`, chave de remote-config): siga o fluxo da seção 6 de `AGENTS.md`.
- **Wear OS:** código do relógio fica em `:wear`; não reutilize `presentation/` de `feature:*` diretamente.
- **Skills relacionadas já existentes:** `feature-architecture`, `wear-architecture`, `navigation-3`, `camerax`, `jetpack-compose-m3`. Use-as para detalhes; prevalece esta skill em conflito.

## Formato de resposta

- Resumo em 1–3 linhas.
- Cite `arquivo:linha` para cada afirmação sobre o código.
- Explique a decisão com base em `AGENTS.md` ou no código existente.
