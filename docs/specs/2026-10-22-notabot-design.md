# notabot — design

Data: 2026-10-22
Objetivo: projeto de treino. A empresa envia um Excel de vendas e o sistema devolve as NF-e (XML + PDF da DANFE) emitidas por um emissor **simulado**.

## Escopo

**Entra na v1**

- Download de um `modelo.xlsx` com as colunas esperadas.
- Upload do Excel, validação por linha e agrupamento das linhas em notas.
- Pré-visualização das notas (válidas e com erro) antes de emitir.
- Emissão simulada: chave de acesso de 44 dígitos com DV real, XML num subconjunto do layout 4.00, número de protocolo e rejeições com códigos reais da SEFAZ.
- Download do XML e do PDF da DANFE por nota, e de um `.zip` com o lote inteiro.

**Fica de fora da v1 (de propósito)**

- Cálculo de impostos. A emissora é uma empresa fictícia fixa, do Simples Nacional, e todo item usa CSOSN 102 (sem destaque de ICMS).
- Login, várias empresas, histórico e banco de dados.
- Emissão real (SEFAZ ou API externa). A interface `Emissor` existe pra isso entrar depois sem mexer no resto.

## Planilha modelo

Uma linha por item. Linhas com o mesmo `pedido` viram uma nota.

| Coluna | Tipo | Regra |
|---|---|---|
| `pedido` | texto | obrigatório |
| `data_emissao` | data | obrigatório, não pode ser futura |
| `cliente_nome` | texto | obrigatório |
| `cliente_documento` | texto | CPF (11) ou CNPJ (14) com DV válido, aceita com ou sem máscara |
| `cliente_uf` | texto | sigla de UF válida |
| `produto_codigo` | texto | obrigatório |
| `produto_descricao` | texto | obrigatório |
| `ncm` | texto | exatamente 8 dígitos |
| `cfop` | texto | exatamente 4 dígitos |
| `quantidade` | número | > 0 |
| `valor_unitario` | número | > 0, até 2 casas decimais |

Todas as linhas de um pedido precisam ter os mesmos `data_emissao`, `cliente_nome`, `cliente_documento` e `cliente_uf`. Se não tiverem, todas as linhas daquele pedido recebem erro.

## Arquitetura

```
notabot/
├── api/   Spring Boot 4.1, Java 21, Maven (wrapper mvnw)
└── web/   Next.js + TypeScript
```

O front chama a API via REST. A API não guarda estado em disco: os lotes ficam num `Map` em memória e somem quando ela reinicia.

### Fluxo

1. `POST /api/lotes/preview` (multipart com o `.xlsx`)
   → `LeitorPlanilha` → `ValidadorLinha` (usa `DocumentoValidator`) → `AgrupadorPedidos`
   → devolve `{ loteId, notas[], erros[] }`. Cada erro tem `{ linha, campo, mensagem }`.
2. `POST /api/lotes/{loteId}/emitir`
   → para cada nota válida: `RegrasRejeicao` → `GeradorChaveAcesso` → `EmissorSimulado` (XML + protocolo)
   → devolve `[{ pedido, chave, status: AUTORIZADA | REJEITADA, codigo, motivo }]`.
3. `GET /api/notas/{chave}/xml`, `GET /api/notas/{chave}/pdf` (gera a DANFE na hora com `GeradorDanfe`), `GET /api/lotes/{loteId}/zip`.

### Módulos da API

Cada pacote tem uma responsabilidade e pode ser testado sozinho. 👤 = o Gabriel implementa (esqueleto + testes JUnit prontos), 🤖 = o Claude implementa.

| Pacote / classe | Responsabilidade | Depende de | Quem |
|---|---|---|---|
| `validacao.DocumentoValidator` | `isCpfValido`, `isCnpjValido`, `somenteDigitos` | nada | 👤 |
| `chave.GeradorChaveAcesso` | monta a chave de 44 dígitos: cUF(2) AAMM(4) CNPJ(14) mod(2)=55 série(3) nNF(9) tpEmis(1)=1 cNF(8) + DV módulo 11 | nada | 👤 |
| `nota.AgrupadorPedidos` | agrupa `LinhaPlanilha` válidas por pedido em `Nota`/`ItemNota` (records) e calcula os totais com `BigDecimal` | `nota` records | 👤 |
| `emissor.RegrasRejeicao` | devolve uma rejeição ou vazio. Casos: pedido que já tem nota autorizada (204, duplicidade de NF-e) e data de emissão com mais de 30 dias de atraso (228, data de emissão muito atrasada) | lista de pedidos já autorizados | 👤 |
| `planilha.LeitorPlanilha` | lê o `.xlsx` com Apache POI e devolve as linhas com o número da linha | Apache POI | 🤖 |
| `planilha.ValidadorLinha` | aplica as regras da planilha modelo e gera os erros | `DocumentoValidator` | 🤖 |
| `emissor.Emissor` / `EmissorSimulado` | interface `emitir(Nota) → ResultadoEmissao`; o simulado gera o XML com StAX (`GeradorXmlNfe`, já vem no Java, sem dependência), o protocolo e guarda os pedidos já autorizados (vale entre lotes, enquanto a API estiver rodando) | `chave`, `RegrasRejeicao` | 🤖 |
| `danfe.GeradorDanfe` | PDF com OpenPDF + código de barras Code128 da chave | OpenPDF | 🤖 |
| `web.*` | controllers, `LoteService` (Map em memória), CORS para `localhost:3000`, `@RestControllerAdvice` | tudo acima | 🤖 |

A numeração `nNF` é sequencial por execução da API e começa em 1. Nota rejeitada não gasta número. O `cNF` é aleatório, com 8 dígitos. A série é sempre 1.

A emitente é fixa (`Emitente.FICTICIA`, CNPJ 11.444.777/0001-61, SP). O XML sai com `tpAmb` 2 (homologação) e a DANFE com a marca d'água "SEM VALOR FISCAL". A API roda na porta **8090**, porque a 8080 é do gym-university.

### Front (`web/`)

| Parte | Quem |
|---|---|
| `lib/api.ts` (cliente tipado da API) | 🤖 |
| Tela de upload (arrastar o arquivo + link pro modelo) | 👤 |
| Tabela de pré-visualização (notas válidas + erros por linha) | 👤 |
| Tela de resultados (status de cada nota, downloads, zip) | 👤 |

## Tratamento de erros

- Arquivo que não é `.xlsx`, ou planilha sem as colunas obrigatórias → **400** com mensagem.
- Erros de linha **não** bloqueiam o lote: as notas válidas seguem, as inválidas aparecem na pré-visualização e não são emitidas.
- `loteId` ou `chave` que não existe, ou zip de lote sem nota autorizada → **404**.
- Planilha só com o cabeçalho → **400**. Arquivo acima de 5 MB → **413**.
- Método de tarefa ainda com `TODO` (`UnsupportedOperationException`) → **501** com a mensagem do TODO.
- Rejeição simulada da SEFAZ **não é erro HTTP**: aparece como `status: REJEITADA` com `codigo` e `motivo`.

## Testes

- 👤: os testes JUnit 5 são entregues antes e falhando. A tarefa é deixá-los verdes, sem mudar os testes.
- 🤖: testes unitários junto com o código. Controller testado com MockMvc, usando uma planilha de exemplo em `src/test/resources`.
- Front: sem testes automatizados na v1. A verificação é manual no navegador.

## Ordem de implementação

1. 🤖 Criar o projeto Spring (Maven wrapper) e os esqueletos + testes de 👤.
2. 👤 `DocumentoValidator`.
3. 👤 `GeradorChaveAcesso`.
4. 👤 `AgrupadorPedidos`.
5. 🤖 `LeitorPlanilha`, `ValidadorLinha`, `EmissorSimulado`, `GeradorDanfe`, API. Os testes dessas partes trocam a `RegrasRejeicao` por uma versão que só checa duplicidade, pra não depender da tarefa 4.
6. 👤 `RegrasRejeicao`.
7. 🤖 Projeto Next + `lib/api.ts`; 👤 as três telas.
8. Revisão de cada parte 👤 com explicação das melhorias.
