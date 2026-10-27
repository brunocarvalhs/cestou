# Template de Prompt — Estrutura de SDD para Novo Repositório

Use este prompt em outro repositório para instruir um agente de IA a criar a infraestrutura completa de **Spec-Driven Development (SDD)** e governança de agentes inspirada no projeto `itau-vn5-dep-ios-access-area`. **Adapte as seções marcadas com `[STACK]`** à linguagem, framework e padrões do repositório destino.

---

## 1. Contexto e Objetivo

Crie a estrutura de governança de agentes de IA e fluxo SDD neste repositório, seguindo o modelo do projeto de referência. O objetivo é garantir que tarefas não triviais sejam planejadas antes de implementadas, com rastreabilidade, revisão técnica e automação padronizada.

O repositório atual usa a stack `[STACK]` (ex.: iOS Swift, Android Kotlin, Node.js, Python, React, etc.). Todas as referências técnicas de exemplo devem ser substituídas pelos padrões, comandos, arquitetura e camadas desta stack.

---

## 2. Estrutura de Arquivos a Criar

Crie os seguintes artefatos na raiz do repositório:

```
<repo-root>/
├── AGENTS.md                                 # fonte de verdade para agentes de IA
├── README.md                                 # adicionar seção "Documentação e SDD"
├── .github/copilot-instructions.md           # instruções para Copilot/agentes (merge com existente)
├── .agents/skills/                           # skills de automação
│   ├── code-planner/SKILL.md
│   ├── code-implementer/SKILL.md
│   ├── code-reviewer/SKILL.md
│   ├── [stack]-architecture/SKILL.md         # ex.: ios-architecture, android-architecture
│   └── open-pull-request/SKILL.md
└── specs/                                    # artefatos SDD versionados
    ├── tasks.txt                             # descrição livre da tarefa atual
    └── sdd-<NNN>-<slug>/
        └── tasks.md                          # plano técnico gerado pelo code-planner
```

> `specs/` deve ser versionado no Git (não adicionar ao `.gitignore`).

---

## 3. Conteúdo de `AGENTS.md`

Crie `AGENTS.md` com as seções abaixo, adaptadas ao repositório destino.

### 3.1 Comandos do Projeto

Liste os comandos essenciais de build, teste, lint e geração de projeto. Exemplo:

| Comando | Descrição | Quando usar |
|---|---|---|
| `[build]` | Builda o projeto/framework. | Validação rápida de compilação. |
| `[test]` | Roda a suíte de testes. | Antes de commit/PR. |
| `[lint]` | Valida estilo de código. | Localmente e na pipeline. |
| `[setup]` | Instala dependências e configura o ambiente. | Primeira vez ou após mudanças em dependências. |

### 3.2 Arquitetura e Padrões

- Descrição de 2–3 linhas do propósito do módulo/repositório.
- Tabela de padrões arquiteturais adotados (Coordinator/MVVM/Clean/Layered/Hexagonal/etc.).
- Camadas lógicas e onde vivem os arquivos.

### 3.3 Princípios de Decisão

- **Código prevalece sobre documentação:** em caso de divergência, valide no código e proponha correção na documentação.
- **Atualização de documentação:** tabela correlacionando tipo de alteração com documentos a atualizar.

### 3.4 Estrutura de Pastas

Diagrama de diretórios da stack, equivalente a:

```
Sources/AccessArea/
├── Classes/
│   ├── Coordinator/
│   ├── Screens/
│   ├── Service/
│   └── ...
Tests/AccessAreaTests/
docs/
├── 01-visao-geral-arquitetura.md
├── 02-regras-de-negocio.md
├── 03-modelos-de-dominio.md
└── 04-integracoes-externas.md
specs/
├── tasks.txt
└── sdd-<NNN>-<slug>/tasks.md
.agents/skills/
├── code-planner/SKILL.md
├── code-implementer/SKILL.md
├── code-reviewer/SKILL.md
├── [stack]-architecture/SKILL.md
└── open-pull-request/SKILL.md
```

### 3.5 Convenções de Código

- Linguagem, plataforma, versões mínimas.
- Padrões de nomenclatura (`PascalCase`, `camelCase`, `kebab-case`).
- Modificadores de acesso e injeção de dependência padrão.
- Regras de cache/persistência, logs, observabilidade e testes.

### 3.6 Fluxo Obrigatório para Novos Parâmetros Públicos

Liste os passos obrigatórios quando houver alteração em contratos públicos (models, protocolos, enums, constantes de API, etc.):

1. Validar necessidade no código e documentação.
2. Marcar explicitamente como `public` / `exported` / equivalente.
3. Adicionar/atualizar testes unitários.
4. Atualizar documentação de modelos e integrações externas.
5. Escrever entrada em `CHANGELOG.md`.
6. Fazer bump de versão se breaking change ou release.
7. Solicitar revisão de, no mínimo, um maintainer.

### 3.7 Instruções para Agentes de IA

- **Co-autoria em commits:** tabela com `Co-authored-by` para cada agente usado (Claude, Copilot, Devin, etc.).
- **Formato de commit:** prefixos (`feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`).
- **Comportamento geral:** nunca expor segredos, confirmar regras no código, citar arquivos e linhas nas justificativas.

### 3.8 Fluxo de Trabalho com SDD

```
1. Escreva a tarefa em `specs/tasks.txt`.
2. Gere o plano técnico invocando a skill `code-planner`.
   - Lê `specs/tasks.txt`, `AGENTS.md`, `.github/copilot-instructions.md` e docs relevantes.
   - Produz `specs/<feature-id>/tasks.md` com as 10 seções definidas.
   - Se houver ambiguidade, o plano sai com `status: blocked` e uma seção "Perguntas em Aberto".
3. Revise o plano com um maintainer (se aplicável).
4. Implemente invocando a skill `code-implementer`.
   - Lê `specs/<feature-id>/tasks.md`, `AGENTS.md` e docs relevantes.
   - Implementa código + testes e roda lint/testes.
   - Encerra indicando que a skill `code-reviewer` deve ser executada.
5. Revise invocando a skill `code-reviewer`.
   - Coleta `git status`, `git diff --staged`, `git diff`, `git log`.
   - Lê arquivos modificados, roda lint e verifica cobertura.
   - Emite conclusão `APROVADO` ou `AJUSTES NECESSARIOS`.
6. (Opcional) Abra o PR invocando a skill `open-pull-request`.
```

> `specs/` é versionado. Não usar `docs/specs/` ou outro caminho legado para entrada de tarefas.

### 3.9 Skills de Automação Git

| Skill | Quando usar |
|---|---|
| `code-planner` | Planejar qualquer tarefa não trivial (antes de codar). |
| `code-implementer` | Implementar a partir de um plano `specs/<feature-id>/tasks.md`. |
| `code-reviewer` | Revisar técnica antes de commit/PR. |
| `[stack]-architecture` | Dúvidas sobre onde criar arquivos, camadas, padrões, parâmetros públicos. |
| `open-pull-request` | **Só quando explicitamente invocada.** Criar PR via ferramenta padrão (gh, az repos, etc.). |

### 3.10 Proibições Globais

- Não versionar segredos, tokens, senhas, chaves de API, certificados, `.env` ou dados sensíveis.
- Não usar `README.md` como fonte de verdade; confirme regras no código e em `docs/`.
- Não commitar código sem testes.
- Não ignorar lint/formatador sem justificar explicitamente.
- Não alterar contratos públicos sem changelog e bump de versão.
- Não fazer `rm -rf`, `git reset --hard`, `git push -f` ou reescrita de história sem confirmação explícita.
- Não carregar `open-pull-request` automaticamente; só quando o usuário pedir.
- Não versionar `specs/` como rascunho oculto; os planos SDD são públicos no repositório.

### 3.11 Dependências Externas

Tabela com framework/biblioteca, versão mínima, uso e onde é referenciado no código.

### 3.12 Contexto do Projeto

- Squad, time, RT, owners, maintainers, plataforma, versão atual, ferramentas de build e release.

---

## 4. Conteúdo de `.agents/skills/`

### 4.1 `code-planner/SKILL.md`

Persona: tech lead sênior especialista na stack `[STACK]`.

**Princípios não negociáveis:**

1. Performance, alocação, threading, cache e concorrência são critérios implícitos.
2. Rastreabilidade obrigatória aos documentos de `docs/`.
3. Questione gaps antes de avançar.
4. Siga `AGENTS.md` e `.github/copilot-instructions.md`.

**Restrições:**

- NÃO escreva código de produção (apenas pseudocódigo, assinaturas, diagramas ASCII).
- NÃO avance com ambiguidade em regras de negócio, dependências, contratos públicos ou escopo.
- NÃO edite arquivos de produção.

**Entrada e Saída:**

- **Entrada:** `specs/tasks.txt` ou input direto do usuário.
- **Saída:** `specs/<feature-id>/tasks.md`.

**Template obrigatório de `specs/<feature-id>/tasks.md`:**

```markdown
# Task: <título curto e imperativo>

- **Slug:** <kebab-case>
- **Autor do plano:** Code-Planner (SDD)
- **Data:** <YYYY-MM-DD>
- **Status:** draft | ready | blocked
- **Versão-alvo:** <ex.: 1.2.0 (minor por feature, patch por fix, major se breaking)>
- **Tipo de mudança:** feature | fix | chore | refactor | breaking-change
- **Impacto público (SDK/API):** none | additive | breaking

## 1. Contexto e Motivação
- Descrição em ≤ 5 linhas do problema/necessidade.
- Referência ao pedido original (`specs/tasks.txt` ou trecho do usuário).
- Rastreabilidade: bullets citando seções relevantes de `docs/01-04` (com número da seção).

## 2. Escopo
- **Inclui** (bullets curtos, verificáveis).
- **Exclui** (o que fica explicitamente fora do escopo).

## 3. Impacto Arquitetural
- Camada afetada.
- Padrão aplicado.
- Estrutura de pastas (novos arquivos).
- Diagrama textual (ASCII) mostrando componentes e fluxo.
- Injeção de dependência e mecanismos de comunicação entre camadas.

## 4. Contratos e Interfaces
- Structs/classes/enums/protocolos alterados ou criados, com assinaturas (sem implementação).
- Marcar campos como `public` / `internal` / `exported` explicitamente.
- Se houver breaking change em contrato público → destacar BREAKING e listar consumidores impactados.
- Novos endpoints, rotas, eventos de observabilidade, timers.
- Referência: `docs/03-modelos-de-dominio.md` e `docs/04-integracoes-externas.md`.

## 5. Regras de Negócio Aplicáveis
Liste em tabela cada regra afetada, com origem em `docs/02`:

| # | Regra atual | Comportamento após a mudança | Origem em `docs/` |
|---|---|---|---|
| 1 | ... | ... | `docs/02-regras-de-negocio.md §X` |

## 6. Critérios de Aceitação
- Performance: alocações, threading, cache correto, evitar I/O duplicado.
- Feature toggles e rollout (se aplicável):
  | Toggle | Default | Fonte | Comportamento OFF | Comportamento ON |
  |---|---|---|---|---|
  | `feat_...` | true/false | backend/local | ... | ... |
- Observabilidade: novos timers/eventos e nome canônico.
- Compatibilidade: versões mínimas, breaking changes, etc.

## 7. Plano de Testes
- **Unit tests:** cenários positivos, negativos (falhas de rede, toggles OFF, cache miss), mocks de dependências externas.
- **Manual/Smoke:** quando aplicável.
- **Lint/formatador:** comando e critério de aprovação.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| ... | baixa/média/alta | ... | toggle, fallback, testes, revisão de maintainer |

- Dependências externas: SDKs, versões mínimas, CLI de PR, etc.

## 9. Perguntas em Aberto (bloqueantes)
- [ ] <pergunta objetiva 1>
- [ ] <pergunta objetiva 2>

Se esta seção não estiver vazia, `status` deve ser **blocked** e a implementação **não pode começar**.

## 10. Checklist de Conformidade
- [ ] Todas as decisões citam ao menos uma seção de `docs/`.
- [ ] Nenhum código de produção foi escrito (apenas pseudocódigo/assinaturas).
- [ ] Seção 6 (Performance/Critérios) preenchida de forma substantiva.
- [ ] Se há mudança em símbolo público, seção 4 e "Impacto público" refletem `breaking`.
- [ ] Se há endpoint novo: gate de toggle definido; ambiente/versão listados; observabilidade planejada.
- [ ] Se há novo cache: storage justificado (volátil vs persistente).
- [ ] Perguntas em aberto foram exauridas.
```

### 4.2 `code-implementer/SKILL.md`

Persona: engenheiro sênior `[STACK]`.

**Princípios:**

1. `specs/<feature-id>/tasks.md` é a fonte da verdade.
2. Não avance com ambiguidade.
3. Respeite `AGENTS.md`, `.github/copilot-instructions.md` e `docs/`.
4. Código prevalece sobre documentação, mas documente o que muda.
5. Testes e lint são obrigatórios.

**Fluxo:**

1. Descubra o `<feature-id>` ativo em `specs/`.
2. Leia `specs/<feature-id>/tasks.md`, `AGENTS.md`, `.github/copilot-instructions.md` e docs citados.
3. Valide `status: ready` e ausência de ambiguidade.
4. Implemente código e testes conforme a arquitetura do projeto.
5. Rode lint e testes; corrija ou justifique violações.
6. Ao final, liste arquivos alterados, resultados de lint/testes e oriente a execução de `code-reviewer`.

### 4.3 `code-reviewer/SKILL.md`

Persona: tech lead sênior de revisão técnica.

**Princípios:**

1. Não altere código.
2. A especificação é a fonte da verdade.
3. Bloqueie por padrão: erros de arquitetura, breaking changes não documentados, falta de testes, violações de lint e divergências da spec.
4. Rastreabilidade das divergências a `docs/` ou `AGENTS.md`.

**Fluxo:**

1. Descubra o `<feature-id>` ativo.
2. Leia `specs/<feature-id>/tasks.md`.
3. Execute e analise: `git status`, `git diff --staged`, `git diff`, `git log --oneline -10`.
4. Liste e leia todos os arquivos modificados (novos e alterados).
5. Valide: alinhamento com a spec, padrões de arquitetura, injeção de dependência, performance/concorrência, regras de negócio, contratos públicos, integrações externas, testes, lint/build, documentação.
6. Execute lint e testes.
7. Emita conclusão: `APROVADO` ou `AJUSTES NECESSARIOS`.

**Formato de resposta:** sumário, divergências bloqueantes, erros de padrão, avisos, pontos positivos, conclusão final.

### 4.4 `[stack]-architecture/SKILL.md`

Persona: especialista de arquitetura `[STACK]`.

**Fonte de verdade (ordem de prioridade):**

1. Código-fonte do projeto.
2. `AGENTS.md`.
3. `.github/copilot-instructions.md`.
4. `docs/01-visao-geral-arquitetura.md`.
5. `docs/02`, `docs/03`, `docs/04` conforme a natureza da pergunta.

**Conteúdo:**

- Mapa arquitetural (árvore de pastas e responsabilidades).
- Camadas e responsabilidades.
- Regras de orientação: onde criar arquivos, como adicionar parâmetros públicos, como escolher a camada certa.
- Formato de resposta: resumo em 1–3 linhas, citação a arquivo/linha, explicação baseada em `AGENTS.md` ou `docs/01`.

### 4.5 `open-pull-request/SKILL.md`

Persona: assistente de automação de Git/PR.

**Atenção:** só carregar quando explicitamente invocada (*progressive disclosure*).

**Pré-condições:**

1. Repositório Git válido com remote configurado.
2. Ferramenta de CLI do forge instalada e autenticada (`gh`, `az repos`, etc.).
3. Branch atual não é a base.
4. Branch atual publicada no remote.
5. Existem mudanças a submeter.

**Fluxo:**

1. Determine a branch base (padrão `main`, `develop` ou equivalente).
2. Valide mudanças (`git status`, `git diff --stat`).
3. Verifique PR já existente.
4. Infira título usando Conventional Commits (`feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`).
5. Gere body com: What, How, Testing, Impact, Checklist.
6. Crie o PR e informe o link.

---

## 5. Conteúdo de `specs/`

### 5.1 `specs/tasks.txt`

Arquivo de entrada livre. Deve conter:

- O que precisa ser feito.
- Por que precisa ser feito (motivação/contexto).
- Qualquer referência a issue, ticket ou documento externo.

Exemplo:

```text
Adicionar validação de CPF no fluxo de cadastro.

Contexto: a tela de onboarding aceita CPFs inválidos hoje, causando erro posterior no backend.
Referência: JIRA-1234.
```

### 5.2 `specs/<feature-id>/tasks.md`

Gerado pelo `code-planner`. Siga o template de 10 seções definido na skill `code-planner`.

- Sugestão de ID: `sdd-<NNN>-<slug>` (ex.: `sdd-001-validacao-cpf`).
- Sobrescreva se já existir, avisando o usuário.
- `status` deve ser `draft`, `ready` ou `blocked`.

---

## 6. Conteúdo de `README.md`

Adicione ou atualize a seção **"Documentação e Spec-Driven Development (SDD)"**.

### 6.1 Seção de Documentação Técnica

Liste os documentos em `docs/` e quando consultar cada um. Exemplo genérico:

| Documento | Consulte quando... |
|---|---|
| `docs/01-visao-geral-arquitetura.md` | precisar entender arquitetura, padrões, camadas ou fluxos macro. |
| `docs/02-regras-de-negocio.md` | for alterar comportamento visível, ações, transições, toggles, alertas. |
| `docs/03-modelos-de-dominio.md` | for tocar em models, enums, protocolos ou constantes públicas. |
| `docs/04-integracoes-externas.md` | for mexer em endpoints, SDKs, rotas, timers ou contrato com host. |

> A fonte de verdade é o código-fonte. O `README.md` é apenas um resumo.

### 6.2 Seção "Como trabalhar com SDD"

```markdown
### Como trabalhar com SDD

Este projeto adota **Spec-Driven Development (SDD)**: antes de implementar uma tarefa não trivial, escreve-se um **plano técnico** que rastreia decisões até as regras documentadas em `docs/`.

**Fluxo recomendado:**

1. **Escreva a tarefa** em `specs/tasks.txt` (texto livre — o que precisa ser feito e por quê).
2. **Gere o plano técnico** invocando a skill [`code-planner`](.agents/skills/code-planner/SKILL.md).
   - A skill lê `specs/tasks.txt` + `AGENTS.md` + `.github/copilot-instructions.md` + docs relevantes.
   - Produz `specs/<feature-id>/tasks.md` seguindo o template SDD.
   - Se houver ambiguidade, o plano sai com `status: blocked` e uma seção **Perguntas em Aberto** — resolva-as antes de implementar.
3. **Revise o plano** com um maintainer.
4. **Implemente** invocando a skill [`code-implementer`](.agents/skills/code-implementer/SKILL.md), seguindo as etapas do `tasks.md`.
5. **Revise** invocando a skill [`code-reviewer`](.agents/skills/code-reviewer/SKILL.md) antes de commit/PR.
6. **(Opcional) Abra o PR** invocando a skill [`open-pull-request`](.agents/skills/open-pull-request/SKILL.md), somente quando solicitado.
7. **Atualize `CHANGELOG.md`** e faça bump de versão se aplicável.

**Convenções do SDD neste repositório:**

- `specs/` é **versionada** e centraliza os artefatos SDD:
  - `specs/tasks.txt` — descrição da tarefa atual.
  - `specs/<feature-id>/tasks.md` — plano técnico gerado por `code-planner`.
- Nenhuma decisão de design entra no plano sem citação a uma seção de `docs/01-04`.
- Performance, alocações, threading, cache e gates de toggle são **critérios de aceitação implícitos**.
- Mudanças em símbolos públicos são **breaking changes** — devem estar explicitamente marcadas no plano e no `CHANGELOG.md`.

**Guia para agentes de IA:** [`AGENTS.md`](AGENTS.md).
```

---

## 7. Instruções Finais para o Agente

Ao aplicar este prompt em um novo repositório:

1. Leia o `README.md`, `package.json`/`Cargo.toml`/`build.gradle`/`pom.xml`/`project.yml`/equivalente para entender a stack e ferramentas.
2. Leia a estrutura de pastas existente e adapte os diagramas e camadas.
3. Substitua todas as ocorrências de `[STACK]` pelo nome da stack do projeto.
4. Substitua todas as referências iOS/Swift/por ambiente pelas correspondentes do projeto destino.
5. Preserve os mecanismos genéricos: `specs/`, `.agents/skills/`, fluxo `code-planner → code-implementer → code-reviewer → open-pull-request`, `AGENTS.md` e co-autoria em commits.
6. Não escreva código de produção nesta tarefa — apenas a estrutura de governança e documentação.
7. Gere `specs/sdd-000-setup-sdd/tasks.md` como primeiro plano, documentando a criação da própria estrutura.
8. Valide que `specs/` não está no `.gitignore`.
9. Execute lint e testes se houver mudanças que afetem arquivos rastreados pelo formatador.
10. Ao final, liste os arquivos criados e oriente a execução de `code-reviewer`.
