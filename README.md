# Carteira de Ações

API REST em **Java 17 + Spring Boot 3.5.14** para gestão de uma carteira de investimentos em ações, com cadastro de corretoras validado por APIs públicas, cotações de mercado (Brasil e EUA), registro de operações de compra e venda, cálculo de preço médio e apuração de resultado. Inclui um painel web estático servido pela própria aplicação.

---

## Sobre o projeto

A aplicação expõe uma API REST (e um painel web que a consome) para:

- **Corretoras** — cadastro a partir apenas de CNPJ e CEP; os dados cadastrais (razão social, nome fantasia, e-mail, telefone, situação cadastral) e o endereço são obtidos em APIs públicas, e a instituição é validada como participante do mercado financeiro pelo CNAE principal.
- **Ações** — cadastro por ticker + mercado (`BRASIL` ou `EUA`); nome da empresa, moeda e cotação vêm da API de cotações correspondente. A cotação pode ser atualizada sob demanda.
- **Operações de compra e venda** — cada operação atualiza a posição da ação e fica registrada em histórico.
- **Preço médio ponderado** — recalculado a cada compra.
- **Resultado realizado** — calculado em cada venda como `(preço de venda − preço médio) × quantidade` e acumulado na ação.
- **Resultado não realizado** — calculado na resposta da ação a partir da cotação atual versus o valor investido.

---

## Tecnologias

Tudo abaixo está declarado no `pom.xml`:

| Tecnologia | Detalhe |
|---|---|
| Java | 17 (`java.version`) |
| Spring Boot | 3.5.14 |
| Spring Web | `spring-boot-starter-web` |
| Spring Data JPA | `spring-boot-starter-data-jpa` (Hibernate) |
| Bean Validation | `spring-boot-starter-validation` |
| Spring Boot DevTools | escopo `runtime`, opcional |
| H2 | banco em memória (perfil padrão) |
| MySQL | driver `mysql-connector-j` (perfil `mysql`) |
| PostgreSQL | driver `postgresql` (perfil `postgresql`) |
| springdoc-openapi | 2.8.9 — Swagger UI / OpenAPI |
| JUnit / Spring Test | `spring-boot-starter-test` |
| Maven | build via `mvnw` / `mvnw.cmd` |
| Docker / Docker Compose | `Dockerfile` multi-stage e `docker-compose.yml` |

O front-end é HTML, CSS e JavaScript puros (`src/main/resources/static/`), sem framework ou build de Node.

---

## Arquitetura

Fluxo principal:

```
Controller  →  Service  →  Repository  →  Entity  →  Banco de dados
                  ↓
            Ports & Adapters (integrações externas)
```

Pacotes em `com.Carteira_de_Acao.demo`:

| Pacote | Conteúdo |
|---|---|
| `controller` | `AcaoController`, `CorretoraController`, `OperacaoController`, `HomeController`, `FrontendController` |
| `service` | `AcaoService`, `CorretoraService`, `OperacaoService` — regras de negócio e transações |
| `repository` | `AcaoRepository`, `CorretoraRepository`, `OperacaoRepository` (Spring Data JPA) |
| `entity` | `Acao`, `Corretora`, `Operacao` |
| `enums` | `Mercado` (BRASIL, EUA), `TipoOperacao` (COMPRA, VENDA) |
| `dto` | `records` de request/response + `ErroResponse` |
| `integration` | portas e adaptadores das APIs externas |
| `exception` | exceções de domínio + `GlobalExceptionHandler` |
| `config` | `RestClientConfig` (bean `RestClient.Builder`) |
| `util` | `CnpjUtil` (dígitos verificadores), `CepUtil` |

### Ports & adapters

As integrações externas ficam atrás de interfaces, com um adaptador por provedor:

```
integration/
  cep/      CepConsultaPort     → BrasilApiCepAdapter
  cnpj/     CnpjConsultaPort    → BrasilApiCnpjAdapter
  cvm/      CvmValidacaoPort    → CnaeCvmValidacaoAdapter
  cotacao/  CotacaoPort         → BrapiCotacaoAdapter (BRASIL)
                                → AlphaVantageCotacaoAdapter (EUA)
            CotacaoServiceFacade — escolhe o adaptador pelo mercado
```

### Validações

- **Bean Validation** nos DTOs de entrada (`@NotBlank`, `@NotNull`, `@Positive`, `@Pattern`, `@Size`) — por exemplo, CNPJ com 14 dígitos e CEP com 8 dígitos.
- **Regras de negócio** nos services: dígitos verificadores do CNPJ, formato do ticker por mercado (`AAAA9`/`AAAA99` para Brasil, 1 a 5 letras para EUA), situação cadastral ativa, unicidade de CNPJ e de ticker, posição suficiente para venda.

### Tratamento de exceções

`GlobalExceptionHandler` (`@RestControllerAdvice`) converte exceções em um corpo `ErroResponse` (`timestamp`, `status`, `erro`, `mensagem`, `path`, `detalhes`):

| Exceção / situação | HTTP |
|---|---|
| `MethodArgumentNotValidException` (validação de DTO) | 400 |
| `RecursoNaoEncontradoException`, rota inexistente | 404 |
| `ConflitoException` (CNPJ ou ticker duplicado) | 409 |
| `RegraNegocioException` | 422 |
| `LimiteRequisicoesException` (rate limit das APIs) | 429 |
| `IntegracaoExternaException` | 502 |
| Demais exceções | 500 |

---

## Funcionalidades

**Corretoras**
- Cadastro informando apenas CNPJ, CEP, número e complemento.
- Validação de formato e de dígitos verificadores do CNPJ; validação de formato do CEP.
- Consulta de CNPJ (razão social, nome fantasia, e-mail, telefone, situação cadastral, CNAE principal).
- Recusa de CNPJ com situação cadastral diferente de ATIVA.
- Validação de participação no mercado financeiro pelo CNAE principal (grupos 6611, 6612, 6619 e correlatos).
- Consulta de CEP para preencher logradouro, bairro, cidade e UF.
- Listagem, busca por id, busca por CNPJ e exclusão — a exclusão é bloqueada quando há ações vinculadas à corretora.

**Ações**
- Cadastro por ticker + mercado, com vínculo opcional a uma corretora.
- Normalização e validação do formato do ticker conforme o mercado.
- Bloqueio de ticker duplicado.
- Busca automática de nome da empresa, moeda e cotação na API do mercado correspondente.
- Listagem, busca por id, busca por ticker e atualização de cotação sob demanda.
- Cada resposta traz quantidade em carteira, preço médio, valor investido, valor atual, resultado não realizado (valor e percentual) e resultado realizado acumulado.

**Operações**
- Compra: recalcula o preço médio ponderado e aumenta a posição. O preço unitário é opcional — sem ele, usa-se a cotação atual da ação.
- Venda: bloqueada quando não há posição ou quando a quantidade excede a posição atual; calcula o resultado realizado, acumula-o na ação e zera o preço médio quando a posição chega a zero.
- Histórico por ação e histórico completo, ordenados da operação mais recente para a mais antiga.

**Painel web**
- Página única em `/painel` (também `/app` e `/dashboard`) com visão geral, ações, operações e corretoras, consumindo a própria API.

---

## Integrações externas

| Finalidade | Serviço | Endpoint usado | Observações |
|---|---|---|---|
| Consulta de CNPJ | **BrasilAPI** | `GET /cnpj/v1/{cnpj}` | Dados da Receita Federal; fornece situação cadastral e CNAE principal. Não requer chave. |
| Consulta de CEP | **BrasilAPI** | `GET /cep/v2/{cep}` | Logradouro, bairro, cidade e UF. Não requer chave. |
| Validação CVM/CNAE | — (local) | — | `CnaeCvmValidacaoAdapter` avalia o CNAE principal retornado pela BrasilAPI; não há chamada HTTP adicional. |
| Cotação — mercado BRASIL | **BRAPI** (`brapi.dev`) | `GET /quote/{ticker}?token=…` | Requer `BRAPI_TOKEN`. Retorna preço, moeda, nome da empresa e horário da cotação. |
| Cotação — mercado EUA | **Alpha Vantage** | `GET /query?function=GLOBAL_QUOTE&symbol=…` | Requer `ALPHAVANTAGE_API_KEY`. Preço em USD; respostas de limite são mapeadas para HTTP 429. |

As URLs base ficam em `application.yml` (`integracao.brasil-api.base-url`, `integracao.brapi.base-url`, `integracao.alphavantage.base-url`).

---

## Variáveis de ambiente

Use o `.env.example` como modelo: copie-o para `.env` na raiz do projeto e preencha os valores. O `.env` está no `.gitignore` e no `.dockerignore`, ou seja, não é versionado nem enviado ao contexto de build da imagem.

```env
BRAPI_TOKEN=
ALPHAVANTAGE_API_KEY=

DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=
```

| Variável | Uso |
|---|---|
| `BRAPI_TOKEN` | Token da BRAPI, para cotações do mercado BRASIL. |
| `ALPHAVANTAGE_API_KEY` | Chave da Alpha Vantage, para cotações do mercado EUA. |
| `DB_HOST` | Host do banco. Usado nos perfis `mysql`/`postgresql`. No Docker Compose, o serviço `app` recebe `DB_HOST=db`, sobrescrevendo o valor do `.env`. |
| `DB_PORT` | Porta do banco. No Compose é usada tanto na publicação da porta do PostgreSQL no host quanto na URL JDBC da aplicação — como o container do banco escuta em 5432, mantenha `DB_PORT=5432`. |
| `DB_NAME` | Nome do banco (no Compose vira `POSTGRES_DB`). |
| `DB_USER` | Usuário do banco (no Compose vira `POSTGRES_USER` e é usado no healthcheck). |
| `DB_PASSWORD` | Senha do banco (no Compose vira `POSTGRES_PASSWORD`). |

As variáveis de banco são necessárias apenas nos perfis `mysql` e `postgresql`; o perfil padrão (`h2`) não precisa delas.

A aplicação importa o `.env` automaticamente quando executada fora do Docker, via `spring.config.import: optional:file:./.env[.properties]` — o arquivo é opcional.

---

## Perfis e bancos de dados

Todos os perfis estão em `src/main/resources/application.yml`. O perfil ativo padrão é `h2`, com `local` incluído (`spring.profiles.include`).

| Perfil | Banco | URL |
|---|---|---|
| `h2` (padrão) | H2 em memória, modo de compatibilidade PostgreSQL | `jdbc:h2:mem:carteira` (usuário `sa`, senha vazia) |
| `mysql` | MySQL | `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:carteira}` |
| `postgresql` | PostgreSQL — perfil usado no Docker | `jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:carteira}` |

Em todos os perfis o schema é gerenciado por `hibernate.ddl-auto: update`. O **H2 Console** fica habilitado apenas no perfil `h2`, em `/h2-console`.

---

## Como executar

### Localmente (perfil padrão H2)

```bash
./mvnw spring-boot:run
```

Outros perfis:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgresql
```

Build do JAR:

```bash
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Com Docker

A aplicação tem suporte completo a Docker.

**`Dockerfile` — build multi-stage:**

1. Estágio `build` com a imagem `maven:3.9-eclipse-temurin-17`: copia o `pom.xml`, baixa as dependências (`mvn dependency:go-offline -B`), copia o `src` e gera o JAR com `mvn clean package -DskipTests`.
2. Estágio final com `eclipse-temurin:17-jre-jammy` (Java 17, apenas JRE): copia o JAR do estágio anterior, expõe a porta **8080** e sobe com `java -jar app.jar`.

O `.dockerignore` mantém fora do contexto de build: `target/`, `.git/`, `.idea/`, `*.iml`, `.env` e `README.md`.

**`docker-compose.yml`** sobe dois containers:

| Serviço | Container | Imagem | Portas |
|---|---|---|---|
| `app` | `carteira-acoes-api` | build local do `Dockerfile` | `8080:8080` |
| `db` | `carteira-acoes-db` | `postgres:16` | `${DB_PORT}:5432` |

- O serviço `app` lê as variáveis do `.env` (`env_file`) e define `SPRING_PROFILES_ACTIVE=postgresql` e `DB_HOST=db` — a comunicação entre os containers acontece pelo nome do serviço `db` na rede criada pelo Compose.
- O PostgreSQL tem **volume persistente** (`postgres_data` em `/var/lib/postgresql/data`) e **healthcheck** com `pg_isready -U ${DB_USER} -d ${DB_NAME}` (a cada 5s, timeout 5s, 10 tentativas).
- O `app` declara `depends_on: db: condition: service_healthy`, ou seja, **só inicia depois que o PostgreSQL está saudável**.

Subir tudo:

```bash
cp .env.example .env    # e preencha os valores
docker compose up --build
```

Parar (mantendo os dados) ou remover também o volume:

```bash
docker compose down
docker compose down -v
```

Fora do Docker o perfil continua sendo o `h2`; o perfil `postgresql` é ativado apenas dentro do container, pela variável de ambiente do Compose.

---

## Endpoints

Base: `http://localhost:8080`

### Raiz e páginas

| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | JSON com nome da aplicação, link da documentação e endpoints principais |
| GET | `/painel`, `/app`, `/dashboard` | Painel web (encaminha para `index.html`) |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/api-docs` | Documento OpenAPI |
| GET | `/h2-console` | Console do H2 (somente perfil `h2`) |

### Corretoras

| Método | Rota | Descrição | Status |
|---|---|---|---|
| POST | `/corretoras` | Cadastra corretora a partir de CNPJ e CEP | 201 |
| GET | `/corretoras` | Lista corretoras | 200 |
| GET | `/corretoras/{id}` | Busca por id | 200 |
| GET | `/corretoras/cnpj/{cnpj}` | Busca por CNPJ | 200 |
| DELETE | `/corretoras/{id}` | Exclui corretora sem ações vinculadas | 204 |

```json
{
  "cnpj": "02332886000104",
  "cep": "04543011",
  "numero": "1000",
  "complemento": "Sala 1"
}
```

### Ações

| Método | Rota | Descrição | Status |
|---|---|---|---|
| POST | `/acoes` | Cadastra ação buscando a cotação no mercado | 201 |
| GET | `/acoes` | Lista ações | 200 |
| GET | `/acoes/{id}` | Busca por id | 200 |
| GET | `/acoes/ticker/{ticker}` | Busca por ticker | 200 |
| PUT | `/acoes/{id}/atualizar-cotacao` | Atualiza cotação, nome da empresa e data/hora | 200 |

```json
{
  "ticker": "PETR4",
  "mercado": "BRASIL",
  "corretoraId": 1
}
```

`mercado` aceita `BRASIL` ou `EUA`; `corretoraId` é opcional.

### Operações

| Método | Rota | Descrição | Status |
|---|---|---|---|
| POST | `/acoes/{id}/comprar` | Registra compra e recalcula o preço médio | 201 |
| POST | `/acoes/{id}/vender` | Registra venda e apura o resultado realizado | 201 |
| GET | `/acoes/{id}/operacoes` | Histórico da ação (mais recentes primeiro) | 200 |
| GET | `/operacoes` | Histórico completo (mais recentes primeiro) | 200 |

```json
{
  "quantidade": 100,
  "precoUnitario": 38.50
}
```

`precoUnitario` é opcional: quando omitido, usa-se a cotação atual da ação — se ela não estiver disponível, a operação é recusada com 422.

---

## Modelo de domínio

```
Corretora 1 ──── 0..* Acao 1 ──── 0..* Operacao
```

| Entidade | Tabela | Campos principais |
|---|---|---|
| `Corretora` | `corretoras` | `cnpj` (único), `razaoSocial`, `nomeFantasia`, `email`, `telefone`, endereço (`cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf`), `situacaoCadastral`, `validadaNaCvm`, `dataCadastro` |
| `Acao` | `acoes` | `ticker` (único), `nomeEmpresa`, `mercado`, `moeda`, `cotacaoAtual`, `dataHoraCotacao`, `corretoraRelacionada` (opcional), `quantidade`, `precoMedio`, `lucroPrejuizoRealizado` |
| `Operacao` | `operacoes` | `acao`, `tipo`, `quantidade`, `precoUnitario`, `valorTotal`, `dataHora`, `resultado` (apenas em vendas), `precoMedioNaOperacao` |

---

## Front-end

SPA servida como conteúdo estático pelo Spring Boot, em `src/main/resources/static/`:

| Arquivo | Conteúdo |
|---|---|
| `index.html` | Estrutura do painel (visão geral, ações, operações, corretoras) |
| `styles.css` | Estilos do painel |
| `app.js` | Consumo da API na mesma origem |

`FrontendController` mapeia `/painel`, `/app` e `/dashboard` para o `index.html`; a rota `/` continua devolvendo o JSON informativo da API.

---

## Testes

```bash
./mvnw test
```

O projeto contém atualmente um teste de contexto (`DemoApplicationTests.contextLoads`).

---

## Coleção Postman

Há uma coleção em `postman/Carteira-de-Acao.postman_collection.json` para importar no Postman.
