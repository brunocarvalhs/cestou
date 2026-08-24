# How Much

![GitHub repo size](https://img.shields.io/github/repo-size/brunocarvalhs/How-Much?style=for-the-badge)
![GitHub language count](https://img.shields.io/github/languages/count/brunocarvalhs/How-Much?style=for-the-badge)
![GitHub top language](https://img.shields.io/github/languages/top/brunocarvalhs/How-Much?style=for-the-badge)

Um aplicativo de carrinho de compras simples, mas poderoso, para Android, projetado para ajudá-lo a gerenciar suas listas de compras, rastrear suas despesas e ficar dentro do seu orçamento.

## ✨ Recursos

*   **Gerenciamento de carrinho de compras:** Crie e gerencie várias listas de compras com facilidade.
*   **Adicionar produtos:** Adicione produtos às suas listas com detalhes como nome, preço e quantidade.
*   **Limite de gastos:** Defina um limite de gastos para cada carrinho de compras para manter seu orçamento sob controle.
*   **Histórico de compras:** Visualize um histórico detalhado de suas compras anteriores para rastrear seus gastos ao longo do tempo.
*   **Compartilhamento de carrinho:** Compartilhe suas listas de compras com amigos e familiares usando um token exclusivo.
*   **Interface de usuário moderna:** Uma interface de usuário limpa e intuitiva construída com Jetpack Compose, seguindo as diretrizes do Material Design 3.

## 📸 Telas (Screenshots)

(Aqui você pode adicionar screenshots do seu aplicativo)

## 🛠️ Tecnologia e Arquitetura

Este projeto foi desenvolvido utilizando as tecnologias e práticas mais recentes do desenvolvimento Android.

*   **Linguagem:** 100% [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) para uma interface de usuário moderna e declarativa.
*   **Navegação:** [Jetpack Navigation 3](https://developer.android.com/jetpack/compose/navigation) com rotas fortemente tipadas (`NavKey`).
*   **Injeção de Dependência:** [Hilt](https://dagger.dev/hilt/) para gerenciar as dependências do projeto.
*   **Arquitetura:** Clean Architecture com padrão MVI (Model-View-Intent) e injeção de IA via `AgentActionUseCase`.
*   **Analytics:** Integração com o Firebase para Analytics, Crashlytics e Performance Monitoring.

### Arquitetura do Projeto

O projeto segue uma arquitetura **Domain-Centric** distribuída em múltiplos módulos:

*   `:core:*`: Módulos transversais que contêm componentes compartilhados (UI, Domain, Data, Navigation).
*   `:feature:*`: Módulos de funcionalidade independentes, cada um seguindo a anatomia:
    - `domain/`: Camada de negócio pura (Modelos, Repositórios, UseCases).
    - `data/`: Camada de infraestrutura (Implementações, DTOs, Mappers).
    - `presentation/`: Camada de UI (MVI com Compose e Data Class Intent).
    - `navigation/`: Definições de rotas (`NavKey`) e grafos da feature.
    - `di/`: Módulos Hilt para provisão de dependências.
*   `:app`: Ponto de entrada do aplicativo que orquestra os módulos de feature via `FeatureInitializer`.

## 🚀 Como Compilar

1.  Clone este repositório:
    ```bash
    git clone https://github.com/brunocarvalhs/How-Much.git
    ```
2.  Abra o projeto no Android Studio.
3.  Sincronize as dependências do Gradle.
4.  Compile e execute o aplicativo em um emulador ou dispositivo Android.

## 🤝 Contribuições

Contribuições são bem-vindas! Sinta-se à vontade para abrir uma issue ou enviar um pull request.

## 📄 Licença

Distribuído sob a licença Apache 2.0. Veja `LICENSE` para mais informações.