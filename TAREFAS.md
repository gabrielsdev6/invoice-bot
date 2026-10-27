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

| # | Classe | Teste | O que treina | Status |
|---|---|---|---|---|
| 1 | `validacao/DocumentoValidator` | `DocumentoValidatorTest` | métodos estáticos, String, laços, `char` → número | ✅ |
| 2 | `chave/GeradorChaveAcesso` | `GeradorChaveAcessoTest` | `String.format`, validação com exceção, `YearMonth` | ✅ |
| 3 | `nota/AgrupadorPedidos` | `AgrupadorPedidosTest` | `Map`, `List`, records, `BigDecimal` | ✅ |
| 4 | `emissor/RegrasRejeicao` | `RegrasRejeicaoTest` | `Optional`, `LocalDate`, injeção de `Clock` | ⏳ |

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

## Ver a API funcionando

Com a tarefa 4 pronta, sobe a API (dentro de `notabot/api`):

```bash
.\mvnw.cmd spring-boot:run
```

Ela fica em `http://localhost:8090`. Em outro terminal (no PowerShell é
`curl.exe`, com `.exe`, porque `curl` sozinho é outro comando lá):

```bash
curl.exe -o modelo.xlsx http://localhost:8090/api/modelo
curl.exe -F "arquivo=@modelo.xlsx" http://localhost:8090/api/lotes/preview
curl.exe -X POST http://localhost:8090/api/lotes/COLE_O_LOTEID_AQUI/emitir
curl.exe -o notas.zip http://localhost:8090/api/lotes/COLE_O_LOTEID_AQUI/zip
```

O `.zip` tem o XML e a DANFE (PDF) de cada nota autorizada. Se emitir o mesmo
lote duas vezes, a segunda vem com rejeição 204 (duplicidade), que é a sua
tarefa 4 trabalhando.

Antes da tarefa 4 ficar pronta, o `emitir` responde **501** com a mensagem
`TODO: tarefa 4`.
