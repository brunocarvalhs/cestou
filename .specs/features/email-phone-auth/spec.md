# Login por e-mail/senha (fase 1) e telefone (fase 2) Specification

Status: Draft (pm, 2026-10-06) — aguardando análise do tech lead
Owner: bruno · Prioridade: **fase 1 é P0, bloqueia a publicação** (ver "Posição no roadmap")

## Fases (decisão do dono, 06/10/2026)

| Fase | Entrega | Requisitos | Desbloqueia |
|---|---|---|---|
| **1** | E-mail e senha (cadastro, login, recuperação) + passo de Nome/Sobrenome com validação + avatar de iniciais + credencial de revisão no Play Console | EPA-01 a EPA-04, EPA-06 a EPA-13 (itens de telefone ficam marcados "fase 2" dentro deles), EPA-12 | A publicação (revisão do Play) |
| **2** | Telefone (SMS): login, código, reenvio, expiração; plano Blaze; Política de Privacidade e Segurança dos dados por coleta de telefone | EPA-05 (inteiro) e os itens "fase 2" dos demais | Ampliar o acesso |

A fase 2 só começa depois que a fase 1 estiver aprovada pelo Play. Nada de telefone entra no build da fase 1
(sem botão, sem provedor ativo no Firebase, sem texto na Política de Privacidade).

## Problema

Em 06/10/2026 o Google Play rejeitou a atualização (faixa Teste aberto, `br.com.brunocarvalhs.howmuch`)
por "Falta de credenciais de login". O app só aceita Google Sign-In e toda rota protegida exige sessão
(`MainActivity.kt`, `isProtectedRoute`), então o revisor não passa da tela de boas-vindas. É a segunda
rejeição pelo mesmo motivo (a de 25/09, versão 9, está em `STATE.md`). Cadastrar credencial no
Play Console é necessário, mas hoje não existe credencial que o app aceite além de uma conta Google real.

Também há um problema de produto: quem não quer ou não pode usar conta Google não consegue entrar.

## Objetivo

1. Desbloquear a revisão do Play: o revisor entra com e-mail e senha de uma conta de revisão dedicada.
2. Ampliar o acesso: e-mail/senha (fase 1) e telefone (fase 2) como alternativas ao Google.

Isto é uma **feature nova, feita do zero com qualidade**, não a restauração do fluxo removido em
29/09/2026 (commits `81f02b73`, `51a931ef`). Esse código serve só de referência de erros a não repetir.

## Validação com personas (skill `customer-personas`)

| Persona | Veredito | Motivo |
|---|---|---|
| Dona Célia (primária de risco) | **serve** se for simples; **atrapalha** se houver muitas opções ou jargão | Provavelmente não tem conta Google ativa, mas tem número de celular. Telefone é o caminho mais natural para ela. Rejeita jargão ("token", "autenticar"). |
| Dona Marlene | serve | Mesmo perfil de Dona Célia; precisa de recuperação simples. |
| Marina | neutro (Google continua rápido) | Rejeita passos extras: Google deve seguir sendo 1 toque. |
| Lucas, Bianca e Diego | serve | Entram numa lista compartilhada: o nome visível importa, ver regra do nome. |
| Rafael, Anderson | serve | Querem conta estável; e-mail/senha é familiar. |
| Yasmin (16) | serve | Pode não ter conta Google própria. |
| Demais (Camila e Pedro, Juliana, Rodrigo, Eduardo) | neutro | Já usam Google; a ordem da tela não pode piorar o caminho deles. |

Nenhuma persona é prejudicada se Google continuar como primeira opção e o fluxo for curto. Conclusão:
faz sentido para o público. Telefone atende melhor as personas de baixa familiaridade; e-mail atende
revisor, Rafael e Anderson.

## Decisões de produto recomendadas

1. **Nome é obrigatório** no cadastro por e-mail e no primeiro acesso por telefone (o campo "Nome de
   Exibição" deixa de ser opcional, não some). Motivo: o nome aparece em lista compartilhada e na autoria
   de itens (`item-add-authorship`), no perfil e nas notificações. Sem nome, o fallback seria o e-mail ou
   o telefone, e isso expõe dado pessoal aos colegas de lista (Lucas, Bianca e Diego). Google já entrega nome.
   Custo para Dona Célia: um campo, aceitável.
   **Decidido pelo dono (2026-10-06): nome obrigatório, com validação contra nomes falsos** (EPA-13).
   **Nova decisão do dono (2026-10-06, substitui "uma palavra basta"): dois campos, Nome e Sobrenome,
   ambos obrigatórios**; o app grava o texto concatenado (EPA-13). Mononímicos: risco conhecido e orientação na UI, ver EPA-13.
2. **Ordem na tela de boas-vindas**: **fase 1**: (1) Continuar com Google (botão principal), (2) Continuar
   com e-mail. **Fase 2**: (1) Google, (2) Continuar com telefone, (3) Continuar com e-mail. Telefone antes de
   e-mail porque atende a persona de maior risco de abandono e não exige criar senha. Cada método é um botão
   de largura total, mesmo peso visual entre os secundários.
3. **Verificação de e-mail não bloqueia o uso** (envia mensagem, mas não impede entrar). Bloquear
   quebraria a conta de revisão e o fluxo da Dona Célia. Reavaliar se houver abuso.
4. **(Fase 2) Telefone não é gravado no documento `users/{uid}`** no Firestore (lido por co-membros, ver AD-009).
   Fica só no Firebase Auth.

## Escopo

Dentro, fase 1:
- Cadastro por e-mail/senha (Nome, Sobrenome, e-mail, senha), login, recuperação de senha por e-mail.
- Passo de Nome/Sobrenome para qualquer conta autenticada sem nome (Google sem `displayName`, contas antigas).
- Avatar com iniciais quando não há foto.
- Telas de boas-vindas/login/cadastro/recuperação no design system do app (`core/ui`).
- Textos em pt-BR, en e es; estados de loading e erro; acessibilidade.
- Testes unitários, de UI e flows Maestro.
- Cadastro da credencial de revisão no Play Console e reenvio da atualização.
- Conferir a Política de Privacidade (e-mail e senha de conta própria; nome e sobrenome) e a declaração de Segurança dos dados.

Dentro, fase 2 (depois da aprovação do Play):
- Login por telefone (número, código SMS de 6 dígitos, reenvio, expiração), com passo de nome no primeiro acesso (reusa o passo da fase 1).
- Plano Firebase Blaze para SMS (decisão do dono), provedor Telefone, números de teste, App Check/reCAPTCHA.
- Atualização da Política de Privacidade e da declaração de Segurança dos dados por coleta de número de telefone.
- Botão de telefone na tela de boas-vindas, flows Maestro e analytics de telefone.

Fora:
- Apple Sign-In, login anônimo/convidado, senha por link mágico, MFA.
- Trocar e-mail/telefone depois de criada a conta; vincular vários métodos à mesma conta (ver questão 4).
- Login no Wear OS (continua pareando com o celular).
- Mudanças em `firestore.rules`/AD-009.

## Requisitos e critérios de aceitação

IDs `EPA-nn`. Todo critério é testável por teste automatizado, Maestro ou checagem manual indicada.

### EPA-01 Tela de boas-vindas
- Fase 1: mostra, nesta ordem, Google e e-mail; cada um com `testTag` próprio. **Fase 2**: Google, telefone, e-mail.
- Google continua entrando em 1 toque (nenhum passo novo para quem já usa).
- Teste de UI confirma a ordem e a presença dos botões da fase; na fase 1 **não existe** botão de telefone.

### EPA-02 Cadastro por e-mail
- Campos, nesta ordem: Nome, Sobrenome, e-mail, senha. **Todos obrigatórios; nenhum campo opcional na tela.**
- Botão "Criar conta" desabilitado enquanto houver campo vazio ou inválido; erro aparece no campo (texto, não só cor) após sair do campo ou ao enviar.
- Nome: validado e normalizado conforme EPA-13.
- E-mail com formato inválido: "Esse e-mail não parece certo".
- Senha com menos de 8 caracteres (mínimo recomendado; tech lead confirma regra do Firebase): erro de senha fraca com a regra dita em palavras; campo com opção de mostrar/ocultar.
- Sucesso: usuário autenticado, perfil gravado com o nome informado, e cai na lista de compras.
- **E-mail já em uso**: mensagem no campo e-mail, com ação "Entrar" que leva ao login com o e-mail preenchido; não revela se a senha existe.
- Sem rede: mensagem de conexão e botão de tentar de novo; formulário preserva o que foi digitado.
- Durante a chamada: botão em loading, campos desabilitados, sem toque duplo (um único pedido).

### EPA-03 Login por e-mail
- Campos: e-mail e senha, ambos obrigatórios; link "Esqueci minha senha"; link "Criar conta".
- Credenciais erradas ou conta inexistente: **mesma** mensagem genérica ("E-mail ou senha incorretos"), sem distinguir os casos.
- Muitas tentativas (bloqueio temporário do Firebase): mensagem própria pedindo para aguardar ou recuperar a senha.
- Conta desativada/removida: mensagem clara, sem travar a tela.
- Conta criada só com Google que tenta e-mail/senha: comportamento definido pela questão 4; nunca erro genérico mudo.

### EPA-04 Recuperação de senha
- Entrada: e-mail (obrigatório, pré-preenchido se vier do login).
- Sucesso e e-mail inexistente mostram a **mesma** confirmação ("Se esse e-mail tiver conta, enviamos um link"), para não permitir enumeração de contas.
- E-mail inválido: erro no campo. Sem rede: erro de conexão com tentar de novo.
- Reenvio permitido, com intervalo de 30 s (botão mostra contagem); sem permitir toque repetido durante o loading.
- Maestro não consegue ler o e-mail: validar o envio por teste unitário do caso de uso e checagem manual na caixa de entrada da conta de teste.

### EPA-05 Login por telefone (FASE 2, fora da fase 1)
- Pré-requisitos: plano Blaze decidido pelo dono, provedor Telefone ativo, números de teste, Política de Privacidade e Segurança dos dados atualizadas para coleta de telefone, tudo antes do primeiro build com o botão.
- Passo 1: número com seletor de país (padrão Brasil, +55) e máscara; número inválido bloqueia o envio com erro no campo.
- Passo 2: código de 6 dígitos (teclado numérico, preenchimento automático do SMS quando disponível); texto mostra o número para o qual foi enviado e permite "Corrigir número".
- **Código inválido**: erro no campo ("Código incorreto, confira e tente de novo"), campo mantém o foco, tentativas seguem permitidas.
- **Código expirado**: mensagem específica ("O código expirou") e ação "Enviar novo código"; entrada do código antigo não autentica.
- **Reenvio**: botão "Reenviar código" desabilitado por 60 s com contagem visível; depois habilita; cada reenvio invalida o código anterior para o usuário (mostra confirmação "Código reenviado").
- Limite de envios excedido (Firebase): mensagem própria ("Muitas tentativas. Tente de novo mais tarde"), sem laço de reenvio.
- Número já tem conta: entra direto, **sem** pedir nome. Número novo: vai ao passo de nome (EPA-06).
- Sem rede e falha de verificação do app (Play Integrity/reCAPTCHA): mensagens distintas e acionáveis.
- Sucesso: autenticado e na lista de compras (ou no passo de nome).

### EPA-06 Passo de Nome e Sobrenome obrigatório
- **Fase 1**: o passo aparece para qualquer usuário autenticado sem nome gravado (Google com `displayName` vazio, conta antiga). **Fase 2**: também após verificar o código de um número de telefone novo. Mostra "Como podemos te chamar?" com 2 campos obrigatórios (Nome e Sobrenome) e botão "Continuar" desabilitado se qualquer um estiver vazio ou inválido.
- Não é possível pular, voltar para o app nem fechar o passo sem nome; se o app for fechado nesse ponto, o próximo acesso retoma neste passo (conta sem nome nunca chega à lista).
- O nome passa pela validação e normalização de EPA-13 antes de ser gravado; "Continuar" só habilita com nome válido.
- O nome é gravado no perfil e aparece em perfil, lista compartilhada, autoria de itens e notificações.

### EPA-07 Regra de campos obrigatórios (transversal)
- Nenhuma tela de cadastro/login tem campo opcional nem rótulo "(opcional)". Verificação: busca nos 3 arquivos de strings por "opcional"/"optional"/"opcional" nessas telas retorna vazio; teste de UI confirma que o envio fica desabilitado com qualquer campo vazio.
- Nenhum usuário autenticado pode ter nome vazio no perfil; contas já existentes sem nome (ex.: criadas pelo fluxo antigo) passam pelo passo de EPA-06 na próxima abertura (migração tratada pelo tech lead).

### EPA-08 Avatar com inicial
- Sem `photoUrl` (e-mail/telefone, ou foto que falhou ao carregar): mostra círculo com as **iniciais**: primeira letra da primeira palavra + primeira letra da última palavra do nome gravado, em maiúscula (ex.: "Ana Souza" mostra "AS"; "Maria da Silva" mostra "MS"; nome de uma palavra só, como vem do Google, mostra 1 letra). Duas letras porque o app agora sempre tem Nome e Sobrenome e a pessoa se reconhece melhor; as iniciais são derivadas do texto único em `users/{uid}.name`, sem campos novos. Cor estável derivada do id/nome (a mesma pessoa tem sempre a mesma cor).
- Só a foto usa o esquema de letra como fallback; nenhum outro lugar inventa avatar (sem ícone genérico de pessoa).
- Mesmo componente em: perfil, membros da lista compartilhada, autoria de item, notificações.
- Nome com emoji ou caractere composto: usa o primeiro caractere visível sem quebrar. Contraste da letra sobre o fundo atende 4.5:1.
- Teste de UI/screenshot cobre: com foto, sem foto, foto com erro.

### EPA-09 Sessão e saída
- Reabrir o app mantém a sessão para os métodos da fase (Google e e-mail; telefone na fase 2). Sair volta às boas-vindas sem estado de formulário anterior.
- Excluir conta (G2/`DeleteAccountUseCase`) funciona para os métodos da fase; se o Firebase exigir login recente, o app guia o usuário em vez de mostrar erro genérico (hoje é lacuna conhecida no F1.1).

### EPA-10 Qualidade de UX, copy e acessibilidade
- Componentes do design system de `core/ui` (campos, botões, tema claro/escuro); sem estilos próprios soltos.
- Strings em `values`, `values-en`, `values-es` para todo texto, inclusive erros e descrições de acessibilidade; nenhum texto fixo no código (lição do `Text("Checkout")` em `STATE.md`). Revisão de copy nos 3 idiomas, linguagem simples, sem jargão (persona Dona Célia).
- Estados: loading, erro, vazio e sucesso em todas as telas.
- Acessibilidade: alvos de toque de pelo menos 48 dp; cada campo com rótulo lido pelo TalkBack; erros anunciados (live region); botão mostrar/ocultar senha com descrição; ordem de foco lógica; funciona com fonte grande (200%) sem cortar texto; teclado certo por campo (e-mail, senha, número).
- Telas usam `testTag`; flows Maestro nunca dependem de texto (o app roda em EN e pt-BR).

### EPA-11 Testes automatizados (parte do pronto)
- Unitários (JVM) para ViewModels e casos de uso: cada estado de erro de EPA-02 a EPA-04 e EPA-06, regra de nome obrigatório, reenvio de recuperação de senha, anti-toque-duplo. **Fase 2**: erros de EPA-05 e contagem de reenvio de SMS.
- Testes de UI Compose: boas-vindas, formulário de cadastro (botão desabilitado), avatar de iniciais. **Fase 2**: telas de número e código SMS.
- Flows Maestro (com `testTag`), fase 1: cadastro por e-mail; login por e-mail; login com senha errada; recuperação de senha (até a confirmação); nome inválido (EPA-13). **Fase 2**: telefone com número de teste do Firebase (código fixo), incluindo código errado e reenvio. Incluídos em `.maestro/test_suite.yaml`.
- Evidência honesta: o que só rodou em JVM não é descrito como verificado em dispositivo (lição de G12–G16). O Maestro precisa de execução real em dispositivo.
- Analytics: eventos de início, sucesso e erro por método (com `data-engineer`), sem enviar e-mail ou telefone como parâmetro; o método "telefone" entra na fase 2.

### EPA-12 Credencial de revisão e reenvio (item operacional, critério de pronto)
- Criar conta dedicada de revisão por e-mail/senha (não usar as 2 contas reais), já com nome preenchido e uma lista com alguns itens para o revisor ver o app com conteúdo.
- Play Console, Conteúdo do app, Detalhes de login: "Sim, partes do app são restritas", com usuário, senha e instrução curta em inglês (Botão "Continue with e-mail", digitar as credenciais). Só e-mail e senha na fase 1; nenhuma instrução de telefone.
- Checar que a conta de revisão entra no build de release (App Check/Play Integrity de release não bloqueia o login em aparelho do revisor).
- Reenviar a atualização na faixa Teste aberto; registrar data e resultado em `STATE.md`.
- Fase 1: conferir que a Política de Privacidade hospedada e a declaração de Segurança dos dados já cobrem e-mail, nome e sobrenome e conta com senha gerida pelo Firebase (o e-mail já é coletado via Google); ajustar se faltar. **Coleta de telefone fica para a fase 2** (atualizar os dois antes do build com o botão de telefone).
- Responsável: bruno (Console) com `android-engineer-release`; o PM só confirma o status.

### EPA-13 Validação do nome (anti-nome-falso)

Princípio: a validação existe para o nome aparecer de forma decente para colegas de lista, não para
provar identidade. Ela deve **errar para o lado de aceitar**: em dúvida, aceita. Uma regra só é
adicionada se não rejeitar nenhum nome da lista de "deve aceitar" abaixo.

**Dois campos, Nome e Sobrenome, ambos obrigatórios** (decisão do dono, 06/10/2026, substitui a versão
"uma palavra basta"). Cumpre a regra "sem campos opcionais". Cada campo é validado separadamente; o app
junta `"<Nome> <Sobrenome>"` (cada um normalizado, um único espaço entre eles) e grava esse texto único
em `displayName` do Firebase Auth e em `users/{uid}.name`. Não há campos separados no Firestore, então
qualquer tela que leia o nome continua funcionando sem mudança. Usuários Google **não** veem esses
campos: o `displayName` do Google é usado como vem (só passa pelo passo de nome se vier vazio, EPA-07).

**Risco conhecido, mononímicos**: pessoas com nome de uma palavra só (comum entre povos indígenas e em
parte da Indonésia, por exemplo) não têm sobrenome. Como nenhum campo pode ser opcional, a UI orienta,
em texto de ajuda sob o campo Sobrenome: "Tem um nome só? Escreva ele de novo aqui." / "Only have one
name? Enter it again here." / "¿Tienes un solo nombre? Escríbelo de nuevo aquí." A validação **aceita**
Nome igual a Sobrenome para permitir isso (grava "Raoni Raoni"). Limitação aceita e a medir no beta;
alternativa (campo Sobrenome opcional) contraria a regra do dono e fica como questão aberta.

Regras de aceitação. Valem para **cada campo** separadamente e, onde indicado, também para o nome
completo concatenado:
1. **Normalização** (por campo, antes de validar e de gravar): remove espaços nas pontas, colapsa espaços internos repetidos em um, aplica Unicode NFC; capitalização: primeira letra de cada palavra em maiúscula e o resto em minúscula, exceto partículas ("da", "de", "do", "dos", "das", "e", "van", "von", "di", "del", "la", "le", "bin") e padrões como "O'Neil", "D'Ávila", "McDonald" preservados quando o usuário já digitou maiúscula interna. Partícula só fica minúscula quando não é a primeira palavra do campo. Exemplo: Nome "  maRIA " + Sobrenome " da   silva" grava "Maria Da Silva" (a partícula abre o campo Sobrenome, então recebe maiúscula; aceito, só estética). O nome completo gravado é sempre `Nome + " " + Sobrenome`.
2. **Tamanho**: cada campo tem de 2 a 30 letras (após normalizar) e no máximo 3 palavras; o nome completo concatenado tem no máximo 60 caracteres (se Nome + Sobrenome passar de 60, o erro aparece no Sobrenome). **"Li" e "Wu" continuam passando** (2 letras); "Jô", "Bo", "Ye" também. Campo de 1 letra ("A", "Y") é rejeitado: limitação conhecida, rara, aceita.
3. **Caracteres permitidos**: letras Unicode (inclui acentos e alfabetos não latinos), espaço, hífen e apóstrofo (`'` e `’`, normalizado para `'`). Hífen e apóstrofo não podem estar no início, no fim, nem repetidos em sequência. Cada palavra tem ao menos uma letra.
4. **Rejeita**: dígitos; emojis e símbolos; qualquer texto com "@", "http", "www", ".com"/domínio, ou aparência de e-mail ou URL; sequência de 7 ou mais dígitos mesmo com separadores (telefone).
5. **Caracteres repetidos** (por campo e no completo): rejeita 3 ou mais letras iguais seguidas ("aaaa", "xxx", "Aaaa"); a regra é por letra repetida, então "Maria", "Anna", "Aaron", "Ee" passam (2 iguais seguidas é permitido). Campo com 2 letras iguais ("Aa") passa.
6. **Sequências de teclado/alfabeto**: rejeita o **campo inteiro** que seja subsequência de 4+ letras de "qwertyuiop", "asdfghjkl", "zxcvbnm", "abcdefgh" ou dessas fileiras invertidas ("asdf", "qwer", "qwerty", "zxcv", "abcd"). Também rejeita o nome completo sem espaços quando tiver 6+ letras e for subsequência de uma dessas fileiras (ex.: Nome "asdf" + Sobrenome "ghjk"). Só rejeita se o campo (ou o completo) for essa sequência, não se a contiver; "Asdrúbal" passa.
7. **Placeholders e termos genéricos** (comparação sem acento e sem maiúscula). **Por campo**: o campo inteiro igual a "teste", "test", "testing", "usuario", "user", "nome", "sobrenome", "name", "surname", "fulano", "beltrano", "ciclano", "tal", "admin", "administrador", "anonimo", "anonymous", "null", "none", "xxx", "asdf", "abc", "nombre", "apellido", "prueba", "ejemplo". **No nome completo**: igual a "fulano de tal", "fulano da silva", "john doe", "jane doe", "nome sobrenome", "first last", "juan perez", "sem nome", "no name", "usuario teste", "test user". Exemplos: "Teste Teste" e "Fulano de Tal" rejeitados; "Teste Silva" rejeitado (campo Nome é placeholder); "Silva Teste" rejeitado pelo mesmo motivo. "nada" fica **fora** da lista: Nada é nome próprio real. O critério é por campo inteiro, não por trecho, para evitar falso positivo.
8. **Palavrões**: lista curta em pt-BR, en e es de termos ofensivos claros, mantida em arquivo de recursos e comparada por **palavra inteira** (nunca por trecho), sem acento e sem maiúscula, para evitar o problema Scunthorpe (ex.: "Cassandra", "Analisa", "Dick" como sobrenome legítimo em en deve ser avaliado: a lista não inclui termos que também são sobrenome comum). A lista é revisada por alguém nativo de cada idioma antes de ir ao ar.
9. **Deve aceitar** (pares Nome / Sobrenome, casos de falso positivo, todos devem passar): "Li" / "Wu"; "Jô" / "Silva"; "Ana" / "Souza"; "Bo" / "Wu"; "Noor" / "Ali"; "João Pedro" / "Santos"; "Ana-Clara" / "Lima"; "Maria" / "da Silva"; "José" / "D'Ávila"; "Sean" / "O'Neil"; "Nguyen" / "Thi Hoa"; "Hans" / "Müller"; "Åsa" / "Berg"; "Raoni" / "Metuktire"; "Raoni" / "Raoni" (mononímico); "Jussara" / "Tupinambá"; "Aaron" / "Anna"; "Nuno" / "Álvares"; "Ye" / "Zé"; "李" / "雷" não (1 letra, ver item 2) mas "李雷" / "韩梅" sim; "Nada" / "Silva".
10. **Deve rejeitar** (casos de teste): Nome vazio; Sobrenome vazio; "   "; "A" / "Silva"; "Ana" / "S"; "aaaa" / "Silva"; "Ana" / "xxx"; "asdf" / "ghjk"; "qwerty" / "Silva"; "Teste" / "Teste"; "Fulano" / "de Tal"; "Test" / "User"; "Usuario" / "Silva"; "Nome" / "Sobrenome"; "Admin" / "Admin"; "joao123" / "Silva"; "João 2" / "Silva"; "😀" / "Silva"; "João😀" / "Silva"; "maria@gmail.com" / "Silva"; "www.site.com" / "Silva"; "11999998888" / "Silva"; "--" / "Silva"; "-Ana" / "Silva"; "Ana--Clara" / "Silva"; Nome de 31 letras; concatenação com 61 caracteres; e um palavrão da lista em qualquer campo.
11. **Mensagens de erro por campo** em pt-BR, en e es, tom de ajuda (nunca "nome falso", "inválido", "proibido"), dizem o que fazer, uma por causa, exibidas abaixo do campo que falhou (o erro de Nome fica no Nome, o de Sobrenome no Sobrenome) e anunciadas pelo leitor de tela. Onde o texto abaixo diz "nome", o campo Sobrenome usa "sobrenome" / "last name" / "apellido":
    - vazio: Nome "Digite seu nome." / "Enter your first name." / "Escribe tu nombre."; Sobrenome "Digite seu sobrenome." / "Enter your last name." / "Escribe tu apellido."
    - muito curto: "Digite pelo menos 2 letras." / "Enter at least 2 letters." / "Escribe al menos 2 letras."
    - caracteres não aceitos (dígito, emoji, símbolo): "Use só letras, espaço, hífen ou apóstrofo." / "Use only letters, spaces, hyphens or apostrophes." / "Usa solo letras, espacios, guiones o apóstrofos."
    - e-mail, link ou telefone: "Aqui vai só o seu nome, sem e-mail, link ou telefone." (+ en, es equivalentes)
    - repetido, sequência, placeholder ou palavrão (mesma mensagem para as quatro, para não ensinar a burlar nem acusar): "Não conseguimos usar esse nome. Que nome seus amigos usam para te chamar?" / "We couldn't use that name. What do your friends call you?" / "No pudimos usar ese nombre. ¿Cómo te llaman tus amigos?"
    - muito longo: "Use no máximo 30 letras." / "Use at most 30 letters." / "Usa como máximo 30 letras."; nome completo acima de 60: erro no Sobrenome "Nome e sobrenome juntos podem ter até 60 letras." / "First and last name together can have up to 60 letters." / "Nombre y apellido juntos pueden tener hasta 60 letras."
    - Os textos da mensagem neutra (repetido/sequência/placeholder/palavrão) trocam "nome" por "sobrenome" no campo Sobrenome; erro do nome completo ("Fulano de Tal") aparece no Sobrenome.
12. **Quando valida**: ao sair do campo e ao enviar; limpa o erro ao digitar. O botão fica desabilitado enquanto qualquer campo estiver inválido ou vazio. Validação não bloqueia digitação (sem filtrar teclas), só avisa.
    **Campos de texto**: `testTag` separados: `signup_given_name_field`, `signup_family_name_field` (cadastro por e-mail) e `name_step_given_name_field`, `name_step_family_name_field` (passo de nome, usado na fase 1 por contas sem nome e na fase 2 também por telefone), mais `..._error` para cada erro. Teclado com `KeyboardCapitalization.Words`, autocorreção desligada; ordem de foco Nome, Sobrenome (depois e-mail e senha no cadastro); ação IME **Next** em todos os campos exceto o último do formulário, que usa **Done** (no passo de nome do telefone, Done no Sobrenome envia o formulário se válido). Autofill hints `personGivenName` (Nome) e `personFamilyName` (Sobrenome), `emailAddress`, `newPassword`. TalkBack lê rótulo "Nome" e "Sobrenome" separadamente.
13. **Onde roda**: uma única função de domínio compartilhada (`core/domain`, sem dependência de Android; recebe Nome e Sobrenome, devolve erro por campo ou o nome completo normalizado) usada por cadastro por e-mail, passo de nome por telefone e edição de nome no perfil (a edição também usa dois campos e a mesma regra, senão a validação é contornável; para contas existentes cujo nome gravado é uma palavra, o primeiro item da lista de Nome/Sobrenome é pré-preenchido com a palavra e o segundo fica vazio). Testes unitários com todos os casos dos itens 9 e 10, mais um teste por regra e por idioma de mensagem (as 3 strings existem).
14. **Reforço no backend**: viável em `firestore.rules` (tech lead decide, ver AD-009). Mínimo viável nas regras de `users/{uid}`: `name` é string, 2 a 60 caracteres, sem dígitos nem "@" (regex simples). A regra é deliberadamente mais frouxa que a do cliente (não replica listas de placeholder/palavrão, que mudam com frequência); o objetivo é barrar escrita direta de lixo, não duplicar a validação. Sem Cloud Functions (plano Spark). Teste nas 60+ asserções do emulador (`firestore-tests/rules.test.mjs`). Se o tech lead julgar que mexer em AD-009 agora aumenta risco de deploy, fica só no cliente, registrado como limitação conhecida.
15. **Limites honestos**: a validação reduz nomes obviamente falsos, não impede "João Silva" inventado. Usuário que acerta uma falsa-negativa não é bloqueado depois. Não há verificação de identidade.
16. **Analytics** (com `data-engineer`): evento de erro de validação de nome com a **categoria** da causa (curto, caractere, contato, bloqueado), nunca o texto digitado. Serve para medir falso positivo: se a taxa de erro "bloqueado" no beta passar de 5% das tentativas, revisar as listas.
17. **Maestro** (por `testTag`): fluxo com Nome "aaaa" vê o erro no campo Nome e botão desabilitado, corrige para "Ana", deixa Sobrenome vazio e vê o erro no Sobrenome, preenche "Silva" e prossegue; roda nos dois idiomas sem depender do texto. Confere também que o nome gravado é "Ana Silva" (perfil).
18. **Concatenação e Google**: teste unitário garante que `displayName` do Auth e `users/{uid}.name` recebem exatamente o mesmo texto concatenado, sem espaços sobrando; usuário Google com `displayName` presente nunca vê os campos.

## Definição de pronto

**Fase 1** (desbloqueia a publicação):
1. EPA-01 a EPA-04, EPA-06 a EPA-13 (sem os itens "fase 2") atendidos; CI verde (Detekt, testes, Kover sem queda).
2. Maestro executado em dispositivo para os flows novos da fase 1.
3. EPA-12 concluído e a revisão do Play aprovada, ou pelo menos reenviada com credencial verificada.
4. Console Firebase conferido: provedor E-mail/Senha ativo, SHA-1/SHA-256 de debug, upload e assinatura do Play registrados.

**Fase 2** (telefone), só depois da aprovação da fase 1:
1. Dono decide o plano Blaze; provedor Telefone ativo, números de teste cadastrados.
2. EPA-05 e itens "fase 2" atendidos, com testes, Maestro em dispositivo e analytics.
3. Política de Privacidade e Segurança dos dados atualizadas para telefone antes do envio do build.

## Questões em aberto (decisão do dono)

1. ~~Plano Firebase e SMS~~ **Adiada para a fase 2 por decisão do dono.** Pendente só nessa fase: o projeto está no plano Spark; SMS em produção costuma exigir Blaze (a confirmar com o tech lead). A fase 1 não depende disso.
2. ~~Faseamento~~ **Fechada pelo dono: fase 1 só e-mail/senha; telefone na fase 2.**
3. ~~Nome obrigatório vs. opcional~~ **Fechada pelo dono: obrigatório, com validação, em dois campos Nome e Sobrenome (EPA-13).** Ainda aberto: aceitar o risco dos mononímicos com a orientação "escreva de novo" (aceita Nome igual a Sobrenome), ou admitir exceção ao "sem campos opcionais"?
4. **Mesmo e-mail em Google e em senha**: vincular as contas automaticamente (exige confirmar posse) ou orientar "entre com Google"? Recomendação: orientar para o método original, com mensagem clara; vincular fica para depois.
5. **Tamanho mínimo da senha** (8) e se haverá verificação de e-mail obrigatória no futuro.
6. **Conta de revisão** em produção: aceitável existir uma conta fixa com senha compartilhada com o Google? Sim, é o padrão; a senha deve ser trocada e a conta apagada ou rotacionada depois da aprovação.
7. **Contas antigas** criadas pelo fluxo removido (se existirem): migrar para o passo de nome? Só 2 contas reais existem hoje, risco baixo.

## Posição no roadmap

- Bloqueia a publicação da atualização, então fica acima de qualquer item da Fase 3 e de "Desejável"; só G9, G5 (deploy das regras) e o merge #110 são decisões do dono que continuam em paralelo.
- Dependência com a linha "Detalhes de login" de `STATE.md` (09-25): a correção do formulário do Console continua sendo ação manual do bruno e agora tem credencial real para preencher.
- Acompanha: Maestro F0.3 (precisa de dispositivo), analytics (`ANALYTICS-PLAN.md`), Política de Privacidade (G3).

## Rastreabilidade

| ID | Tema | Status |
|---|---|---|
| EPA-01..EPA-04, EPA-06..EPA-13 | Fase 1 (itens de telefone dentro deles são fase 2) | Pending (design/tarefas a cargo do tech lead) |
| EPA-05 | Fase 2, telefone | Adiado |
