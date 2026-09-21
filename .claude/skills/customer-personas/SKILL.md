---
name: customer-personas
description: Confronta uma ideia de funcionalidade com perfis fake de clientes (personas) do Cestou/How Much antes de virar spec. Use quando o usuário trouxer uma ideia nova de feature, pedir para validar/priorizar algo do ponto de vista do cliente, ou perguntar "isso faz sentido pra quem usa o app?".
---

# Personas de clientes do Cestou (How Much)

Este skill existe para apoiar Spec-Driven Development: antes de transformar uma ideia em spec,
confronte-a com os perfis abaixo. O objetivo não é aprovar/reprovar sozinho — é forçar a pergunta
"pra qual desses clientes isso resolve um problema real, e pra qual atrapalha?" e deixar isso
explícito na spec.

## Como usar

Quando o usuário trouxer uma ideia de funcionalidade:

1. Para cada persona abaixo, avalie rapidamente: **serve** (resolve uma dor real dela),
   **neutro** (não muda nada pra ela) ou **atrapalha** (adiciona fricção/complexidade que ela não quer).
2. Identifique a persona *primária* da ideia — geralmente é óbvio pelo problema que motivou a ideia.
3. Se a ideia atrapalha uma persona relevante (ex: adiciona passos pra Dona Célia, ou quebra o
   compartilhamento de carrinho do Lucas), aponte isso antes de prosseguir — não é bloqueio automático,
   é um trade-off a decidir conscientemente.
4. Não invente personas novas ad-hoc na conversa. Se nenhuma persona existente serve para avaliar a
   ideia, proponha adicionar uma nova ao arquivo (ver seção final) em vez de avaliar no vácuo.

## Personas

### Marina, 34 — mãe, orçamento familiar apertado
- **Contexto:** duas crianças, faz a compra do mês no mercado, cada real fora do orçamento dói.
- **Objetivo no app:** não estourar o limite de gastos do carrinho; comparar preço com a compra anterior.
- **Dores:** perde tempo somando na calculadora; descobre que passou do orçamento só no caixa.
- **O que ela valoriza:** alertas de limite de gastos, histórico de preço por produto, clareza total do total corrente.
- **O que ela rejeita:** qualquer fluxo com passos extras antes de ver o total do carrinho.

### Lucas, 26 — mora com colegas de apartamento, divide despesas
- **Contexto:** compra de mercado dividida entre 2-3 pessoas, usa o token de compartilhamento de carrinho.
- **Objetivo no app:** todo mundo ver e editar a mesma lista em tempo real, sem confusão de quem já comprou o quê.
- **Dores:** duplicidade de item (dois colegas compram a mesma coisa), briga por causa de rateio impreciso.
- **O que ele valoriza:** compartilhamento de carrinho, marcação de quem adicionou/comprou cada item.
- **O que ele rejeita:** qualquer feature que só funcione bem com um único usuário no carrinho.

### Dona Célia, 68 — aposentada, pouca familiaridade com apps
- **Contexto:** faz compra pequena e frequente, usa o celular do jeito que aprendeu, sem pressa mas sem paciência pra passo extra.
- **Objetivo no app:** anotar o que precisa comprar e saber quanto vai gastar, do jeito mais direto possível.
- **Dores:** telas com muitas opções, texto pequeno, termos técnicos ("token", "sincronizar").
- **O que ela valoriza:** poucos toques até a ação, texto grande e claro, funcionar bem mesmo com conexão ruim.
- **O que ela rejeita:** qualquer feature que exija entender um conceito novo antes de usar (ex: fluxo com jargão).

### Rafael, 41 — pequeno revendedor, usa o app para custo de reposição
- **Contexto:** compra em atacado/mercado para revender, precisa saber o custo exato de cada compra.
- **Objetivo no app:** histórico de compras confiável e detalhado, capaz de virar controle financeiro simples.
- **Dores:** precisa lembrar de cabeça o preço de custo; sem histórico correto, erra o preço de venda.
- **O que ele valoriza:** histórico de compras preciso, granularidade por produto/quantidade/preço.
- **O que ele rejeita:** simplificações que escondam detalhe (ex: arredondar valores, agrupar itens sem discriminar).

### Dona Marlene, 56 — mãe de 3 filhos, orçamento fixo do marido aposentado
- **Contexto:** faz a compra do mês em mercados de atacado para economizar; o dinheiro é fixo, repassado
  pelo marido aposentado, sem folga — se passar do valor, precisa devolver produto no caixa na frente de todo mundo.
- **Objetivo no app:** ter certeza do total antes de chegar no caixa e comparar qual produto vale mais
  (custo-benefício entre marcas/tamanhos), do jeito que hoje faz de cabeça olhando as prateleiras.
- **Dores:** já passou pelo constrangimento de devolver item no caixa por estourar o orçamento; hoje
  carrega uma lista de papel do que falta em casa só para não comprar duplicado ou em excesso.
- **O que ela valoriza:** total do carrinho sempre visível e atualizado a cada item adicionado; comparação
  de preço entre produtos parecidos; lista de compras confiável do que realmente falta.
- **O que ela rejeita:** qualquer fluxo em que o total fique escondido até o fim, ou que dificulte comparar
  dois produtos lado a lado antes de decidir qual colocar no carrinho.

### Bianca e Diego, 30 anos — casados há 7 anos, compram juntos
- **Contexto:** casal, fazem compra em atacado e também no mercado do bairro para controlar gasto; vão
  juntos ao mercado, um fica com o carrinho enquanto o outro tenta lembrar o que falta em casa.
- **Objetivo no app:** ter uma única lista viva durante a compra que os dois enxerguem e editem ao mesmo
  tempo, sem depender de lembrar de cabeça nem de avisar o outro por fora.
- **Dores:** hoje coordenam pelo WhatsApp, mas o assunto se perde entre vários chats e vira motivo de
  atrito; já tentaram lista (de papel) antes e não pegou, então continuam comprando "de memória".
- **O que valorizam:** lista compartilhada atualizada em tempo real para os dois enquanto estão juntos
  no mercado, sem precisar sair do app para confirmar algo pelo chat.
- **O que rejeitam:** qualquer solução que dependa de um canal separado (chat, ligação) para os dois
  ficarem alinhados sobre o que já foi pego e o que ainda falta.

### Anderson, 39 — MEI, dono de pizzaria
- **Contexto:** compra insumos em grande volume em mercados de atacado; hoje guarda todos os recibos de
  papel e, no fim do mês, junta tudo e lança à mão numa planilha Excel para saber quanto gastou em material.
- **Objetivo no app:** saber o gasto com insumos já consolidado, sem precisar rearmar essa conta manualmente
  a cada fechamento de mês.
- **Dores:** perde boa parte do fim de semana somando recibo por recibo numa planilha; recibo perdido ou
  rasurado vira gasto que não entra na conta; só descobre o custo real do mês depois de fechado, não durante.
- **O que ele valoriza:** histórico de compras com data e valor que já chega pronto para virar total do mês,
  sem exigir digitação dupla do que já foi comprado.
- **O que ele rejeita:** qualquer fluxo que recrie o mesmo trabalho manual que ele já faz no Excel (ex:
  precisar digitar cada item de novo em vez de aproveitar o que já foi registrado na compra).

### Yasmin, 16 — adolescente, quer cozinhar uma surpresa para a família
- **Contexto:** decide preparar um prato especial de surpresa, acha uma receita num site, vai ao mercado
  do bairro comprar os ingredientes com base nela.
- **Objetivo no app:** chegar em casa e ainda saber exatamente qual receita motivou aquela compra, para
  poder seguir o passo a passo com os ingredientes certos em mãos.
- **Dores:** perde a referência da receita entre a ida ao mercado e a volta pra casa (fecha a aba, esquece
  o link); em casa, não lembra qual era e acaba procurando outra receita com o que tem, gerando ingrediente
  comprado à toa e resultado diferente do planejado.
- **O que ela valoriza:** conseguir guardar, junto da lista de compras, o motivo/origem daquela lista (a
  receita que gerou aqueles itens), não só os itens soltos.
- **O que ela rejeita:** uma lista de compras que trata os itens como soltos, sem nenhum vínculo com o
  "para quê" ela foi montada.

### Camila e Pedro, 24 anos — recém-casados, começando a morar juntos
- **Contexto:** acabaram de se casar e estão montando a rotina da casa nova juntos; é a primeira vez que
  cada um decide sozinho (sem a casa dos pais como referência) o que cozinhar e o que precisa comprar.
- **Objetivo no app:** ter algum ponto de partida do que costuma compor uma compra de mercado básica, em
  vez de montar a lista do zero sem saber o que estão esquecendo.
- **Dores:** vão ao mercado sem saber direito o que realmente precisam, compram por impulso ou esquecem
  item essencial, e só percebem a falta no meio da semana.
- **O que valorizam:** alguma sugestão ou modelo inicial de lista (itens básicos de despensa/rotina) que
  ajude a não começar cada compra do zero.
- **O que rejeitam:** um app que assume que eles já sabem exatamente o que montar, sem nenhuma sugestão
  ou apoio inicial para quem está começando essa rotina agora.

### Juliana, 27 — solteira, mora sozinha, não gosta de escrever lista
- **Contexto:** não gosta de digitar/escrever lista de compras; tem o hábito de tirar foto do armário da
  cozinha antes de ir ao mercado, e depois tenta lembrar de cabeça, olhando a foto, o que está faltando.
- **Objetivo no app:** saber o que precisa comprar e entender o que consome mais ao longo dos meses, sem
  precisar digitar tudo manualmente toda vez.
- **Dores:** depende da memória pra traduzir a foto do armário em lista, então erra e esquece item; não
  tem noção nenhuma do próprio padrão de consumo (o que compra com mais frequência, o que sobra parado).
- **O que ela valoriza:** um jeito de registrar o que tem/falta que não seja digitar texto (ex: foto,
  toque rápido em vez de escrita); alguma visão do que ela mais consome ao longo do tempo.
- **O que ela rejeita:** qualquer fluxo de montar a lista que dependa só de digitação manual, sem
  alternativa mais rápida.

### Rodrigo, 37 — solteiro, fitness, faz marmita em casa
- **Contexto:** faz a compra do mês sempre com o mesmo perfil de produtos, monta marmitas variadas em
  casa pra manter a dieta; tem uma lista fixa de itens que sempre precisa e vive imprimindo esse
  checklist antes de ir ao mercado.
- **Objetivo no app:** ter uma lista recorrente salva que reutiliza mês a mês, sem precisar recriar ou
  imprimir do zero toda vez.
- **Dores:** gasta com tinta e papel toda vez que imprime a mesma lista de sempre; refaz um trabalho que
  já devia estar pronto, já que a lista básica quase não muda.
- **O que ele valoriza:** um modelo/lista reutilizável de compra recorrente, com item marcável/desmarcável
  sem precisar de papel.
- **O que ele rejeita:** um app que só serve para lista avulsa e descartável, obrigando a montar tudo de
  novo a cada compra.

### Eduardo, 42 — casado, contador, economiza até centavo
- **Contexto:** contador, gosta de economizar até no centavo; no mercado, compara na calculadora do
  celular o preço por unidade entre variações do mesmo produto (ex: pacote de 4 pastas de dente vs. 4
  unidades soltas, diferença de 25 centavos) antes de decidir o que leva.
- **Objetivo no app:** comparar rápido o preço por unidade entre variações de um mesmo produto, sem
  precisar fazer essa conta manualmente item por item.
- **Dores:** essa comparação item a item toma horas no mercado — já perdeu o dia inteiro de compra por
  causa disso; a economia de centavos é real, mas o tempo gasto pra chegar nela é desproporcional.
- **O que ele valoriza:** comparação de preço por unidade automática entre variações de produto, que
  substitua a conta manual na calculadora.
- **O que ele rejeita:** qualquer fluxo que não ajude a comparar preço por unidade — pra ele, se ainda
  precisa pegar a calculadora à parte, o problema não foi resolvido.

## Adicionando uma persona nova

Quando uma ideia de feature não se encaixa em nenhum público acima (ex: recurso social, recurso B2B),
proponha ao usuário uma persona nova antes de avaliar a ideia contra ela. Siga o mesmo formato: contexto,
objetivo no app, dores, o que valoriza, o que rejeita — 5 a 8 linhas, específico o bastante para gerar
um "sim/não" claro quando confrontado com uma ideia real.
