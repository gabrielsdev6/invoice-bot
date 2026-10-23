# Tarefas do Gabriel 👤

Cada tarefa é uma classe com `TODO`. Dentro dela tem as regras e, no fim, dicas
pra você ler só se travar. Os testes já estão prontos: **a tarefa termina quando
eles ficam verdes, sem mexer nos testes.**

## Como rodar

No terminal, dentro de `notabot/api`:

```bash
./mvnw test -Dtest=DocumentoValidatorTest
```

Troque o nome do teste pra cada tarefa. A primeira vez demora, porque o
`mvnw` baixa o Maven e as dependências. No Windows (PowerShell ou CMD) use
`mvnw.cmd` no lugar de `./mvnw`.

Pra rodar tudo de uma vez: `./mvnw test`.

## Ordem

| # | Classe | Teste | O que treina |
|---|---|---|---|
| 1 | `validacao/DocumentoValidator` | `DocumentoValidatorTest` | métodos estáticos, String, laços, `char` → número |
| 2 | `chave/GeradorChaveAcesso` | `GeradorChaveAcessoTest` | `String.format`, validação com exceção, `YearMonth` |
| 3 | `nota/AgrupadorPedidos` | `AgrupadorPedidosTest` | `Map`, `List`, records, `BigDecimal` |
| 4 | `emissor/RegrasRejeicao` | `RegrasRejeicaoTest` | `Optional`, `LocalDate`, injeção de `Clock` |

Os caminhos são a partir de `api/src/main/java/com/gabrielsdev6/notabot/`.

## Como ler um teste que falhou

- `UnsupportedOperationException: TODO` → você ainda não implementou o método.
- `expected: true but was: false` → o resultado está errado. O nome do teste
  (ex.: `Cpf > invalidos("111.111.111-11")`) diz qual caso quebrou.
- `Expecting code to raise a throwable` → era pra lançar
  `IllegalArgumentException` e não lançou.

## Quando terminar uma

1. Rode o teste e veja tudo verde.
2. Faça um commit pequeno, por exemplo: `git commit -m "Valida CPF e CNPJ"`.
3. Me avise pra eu revisar e explicar o que dá pra melhorar.

Enquanto você faz as tarefas 1 a 3, eu não mexo nesses arquivos. Depois da 3, eu
monto a leitura da planilha, o emissor simulado, a DANFE e a API.
