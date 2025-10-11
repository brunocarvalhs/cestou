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
*   **Navegação:** [Jetpack Navigation](https://developer.android.com/jetpack/compose/navigation) para gerenciar a navegação entre as telas do aplicativo.
*   **Injeção de Dependência:** [Hilt](https://dagger.dev/hilt/) para gerenciar as dependências do projeto.
*   **Arquitetura:** Arquitetura Limpa (Clean Architecture) com uma estrutura multi-módulo (`'app'`, `'data'`, `'domain'`).
*   **Analytics:** Integração com o Firebase para Analytics, Crashlytics e Performance Monitoring.

### Arquitetura do Projeto

O projeto é dividido nos seguintes módulos:

*   `:app`: Contém a camada de UI, incluindo todas as telas (Composables), `MainActivity`, e a lógica de navegação.
*   `:data`: Responsável pelas fontes de dados, como banco de dados local e serviços de rede. Implementa os repositórios definidos no módulo de domínio.
*   `:domain`: Contém a lógica de negócios principal do aplicativo, incluindo casos de uso (use cases), entidades e as interfaces dos repositórios.

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