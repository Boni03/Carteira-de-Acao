# Carteira de Ação

API REST em **Java Spring Boot** para gestão de corretoras e ações financeiras, com integração a APIs públicas para validação e enriquecimento de dados.

## Requisitos

- Java 17+
- Maven 3.9+

## Configuração das chaves de API

Copie o exemplo e preencha suas chaves (ou use variáveis de ambiente):

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

```properties
integracao.brapi.token=SUA_CHAVE_BRAPI
integracao.alphavantage.api-key=SUA_CHAVE_ALPHAVANTAGE
```

Alternativa por variáveis de ambiente:

- `BRAPI_TOKEN`
- `ALPHAVANTAGE_API_KEY`

O arquivo `application-local.properties` está no `.gitignore` e não deve ser commitado.

## Executar

```bash
# H2 (padrão)
./mvnw spring-boot:run

# MySQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2,mysql

# PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2,postgresql
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Console H2: `http://localhost:8080/h2-console` (JDBC: `jdbc:h2:mem:carteira`, user `sa`, senha vazia)

## Endpoints

### Corretoras

| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/corretoras` | Cadastra corretora (CNPJ + CEP validados externamente) |
| GET | `/corretoras` | Lista corretoras |
| GET | `/corretoras/{id}` | Busca por id |
| GET | `/corretoras/cnpj/{cnpj}` | Busca por CNPJ |

**Exemplo cadastro:**

```json
{
  "cnpj": "02332886000104",
  "cep": "04543011",
  "numero": "1000",
  "complemento": "Sala 1"
}
```

### Ações

| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/acoes` | Cadastra ação com cotação da API do mercado |
| GET | `/acoes` | Lista ações |
| GET | `/acoes/{id}` | Busca por id |
| GET | `/acoes/ticker/{ticker}` | Busca por ticker |
| PUT | `/acoes/{id}/atualizar-cotacao` | Atualiza cotação |

**Exemplo ação brasileira:**

```json
{
  "ticker": "PETR4",
  "mercado": "BRASIL"
}
```

**Exemplo ação americana:**

```json
{
  "ticker": "AAPL",
  "mercado": "EUA"
}
```

## APIs externas utilizadas

| Finalidade | API | URL base | Limitações |
|------------|-----|----------|------------|
| CNPJ / Receita | [Brasil API](https://brasilapi.com.br) | `https://brasilapi.com.br/api/cnpj/v1/{cnpj}` | Rate limit; dados dependem da base pública |
| CEP | [Brasil API](https://brasilapi.com.br) | `https://brasilapi.com.br/api/cep/v2/{cep}` | CEP inexistente retorna 404 |
| Validação mercado financeiro | CNAE via dados do CNPJ (Brasil API) | — | Critério acadêmico equivalente à CVM; CNAEs 6611/6612/6619 |
| Cotação B3 | [brapi.dev](https://brapi.dev) | `https://brapi.dev/api/quote/{ticker}` | Requer token; limite de requisições no plano gratuito |
| Cotação EUA | [Alpha Vantage](https://www.alphavantage.co) | `GLOBAL_QUOTE` | 5 req/min no plano gratuito; delay entre chamadas |

## Arquitetura

Camadas: `controller` → `service` → `repository` → `entity`, com `dto` e integrações isoladas por **ports/adapters**:

```
integration/
  cnpj/   CnpjConsultaPort → BrasilApiCnpjAdapter
  cep/    CepConsultaPort → BrasilApiCepAdapter
  cvm/    CvmValidacaoPort → CnaeCvmValidacaoAdapter
  cotacao/ CotacaoPort → BrapiCotacaoAdapter | AlphaVantageCotacaoAdapter
           CotacaoServiceFacade (Strategy por mercado)
```

Tratamento de erros centralizado em `GlobalExceptionHandler`.

## Diagrama simplificado das entidades

```
┌─────────────┐       opcional      ┌─────────────┐
│  Corretora  │◄────────────────────│    Acao     │
├─────────────┤                     ├─────────────┤
│ id          │                     │ id          │
│ cnpj (UK)   │                     │ ticker (UK) │
│ razaoSocial │                     │ nomeEmpresa │
│ cep, endereço│                    │ mercado     │
│ validadaNaCvm│                    │ moeda       │
│ dataCadastro │                    │ cotacaoAtual│
└─────────────┘                     │ dataHoraCot.│
                                    └─────────────┘
```

## Tratamento de falhas

| Cenário | HTTP |
|---------|------|
| CNPJ/CEP/ticker não encontrado | 404 |
| CNPJ inválido / não validado CVM / situação inativa | 422 |
| Duplicidade CNPJ ou ticker | 409 |
| API externa indisponível | 502 |
| Limite de requisições (Brapi/Alpha Vantage) | 429 |
