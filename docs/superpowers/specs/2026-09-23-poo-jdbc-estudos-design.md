# Design — poo-jdbc-estudos

## Contexto e objetivo

Projeto de estudo em Java para praticar Programação Orientada a Objetos (POO)
combinada com acesso a banco de dados via JDBC. Não é software de produção —
o critério de sucesso é o aprendizado dos conceitos: encapsulamento,
interfaces/polimorfismo, separação de responsabilidades em camadas,
`PreparedStatement`, transações JDBC e tratamento de exceções.

Domínio escolhido: sistema de biblioteca (livros, usuários e empréstimos).

## Arquitetura

Três camadas, com o padrão DAO (Data Access Object) isolando o acesso ao
banco do restante da aplicação:

```
poo-jdbc-estudos/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/java/br/com/lucindo/biblioteca/
    │   ├── Main.java
    │   ├── model/
    │   │   ├── Livro.java
    │   │   ├── Usuario.java
    │   │   └── Emprestimo.java
    │   ├── dao/
    │   │   ├── LivroDAO.java
    │   │   ├── UsuarioDAO.java
    │   │   ├── EmprestimoDAO.java
    │   │   └── sqlite/
    │   │       ├── LivroDAOSQLite.java
    │   │       ├── UsuarioDAOSQLite.java
    │   │       └── EmprestimoDAOSQLite.java
    │   ├── service/
    │   │   └── EmprestimoService.java
    │   ├── exception/
    │   │   └── RepositorioException.java
    │   └── config/
    │       └── ConexaoFactory.java
    └── main/resources/
        └── schema.sql
```

- **model**: classes de domínio (POJOs), com atributos privados e
  getters/setters — sem lógica de acesso a dados.
- **dao**: uma interface por entidade define os métodos permitidos
  (`salvar`, `buscarPorId`, `listarTodos`, `atualizar`, `deletar`, e métodos
  específicos como `listarDisponiveis` ou `listarAtivos`). A implementação
  concreta (`*DAOSQLite`) contém o SQL real via JDBC.
- **service**: `EmprestimoService` concentra regras de negócio que
  atravessam mais de uma entidade (empréstimo e devolução de livro).
- **config**: `ConexaoFactory` centraliza a criação de `Connection` SQLite
  (arquivo `biblioteca.db` na raiz do projeto).
- **exception**: `RepositorioException` (unchecked) encapsula
  `SQLException`, evitando que detalhes do driver JDBC vazem para camadas
  superiores.
- **Main**: menu de console (`Scanner`) que orquestra os casos de uso.

## Modelo de dados

Schema criado automaticamente na primeira execução via `schema.sql`
(executado pela `ConexaoFactory` se as tabelas não existirem):

```sql
CREATE TABLE IF NOT EXISTS livro (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL,
    autor TEXT NOT NULL,
    isbn TEXT UNIQUE NOT NULL,
    quantidade_total INTEGER NOT NULL,
    quantidade_disponivel INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS emprestimo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    livro_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,
    data_emprestimo TEXT NOT NULL,
    data_prevista_devolucao TEXT NOT NULL,
    data_real_devolucao TEXT,
    status TEXT NOT NULL, -- 'ATIVO' ou 'DEVOLVIDO'
    FOREIGN KEY (livro_id) REFERENCES livro(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
```

## Regras de negócio (EmprestimoService)

- **Registrar empréstimo**: só permitido se `livro.quantidade_disponivel > 0`.
  Ao registrar, decrementa `quantidade_disponivel` do livro e cria o
  registro de empréstimo com `status = 'ATIVO'` e prazo de devolução de
  14 dias a partir da data atual.
- **Registrar devolução**: calcula multa de R$ 2,00 por dia de atraso
  (diferença entre data real e `data_prevista_devolucao`, se positiva).
  Atualiza `emprestimo` (`data_real_devolucao`, `status = 'DEVOLVIDO'`) e
  incrementa `quantidade_disponivel` do livro **na mesma transação JDBC**
  (`Connection.setAutoCommit(false)`, commit ao final, rollback em caso de
  exceção).

## Fluxo principal (Main)

Menu de console com as opções:
1. Cadastrar livro
2. Cadastrar usuário
3. Registrar empréstimo
4. Registrar devolução (exibe multa, se houver)
5. Listar livros disponíveis
6. Listar empréstimos ativos
0. Sair

## Tratamento de erros

- Exceções checked do JDBC (`SQLException`) são capturadas na camada DAO e
  relançadas como `RepositorioException` (unchecked), com mensagem
  contextual.
- Regras de negócio violadas (ex.: tentar emprestar livro sem exemplar
  disponível) lançam `IllegalStateException` a partir do `EmprestimoService`,
  capturada no `Main` para exibir mensagem amigável ao usuário sem encerrar
  o programa.

## Testes

Fora de escopo nesta primeira versão — o foco é a prática de JDBC/POO em
si. Pode ser adicionado (JUnit + banco em memória) em uma iteração futura,
caso o projeto evolua além do aprendizado inicial.

## Build e dependências

- **Build tool**: Maven (`pom.xml`), a ser instalado no ambiente via `apt`
  caso ainda não esteja disponível.
- **Dependência JDBC**: `org.xerial:sqlite-jdbc` (driver SQLite puro Java,
  não requer servidor de banco).
- **Java**: versão 21 (já disponível no ambiente).

## Git e GitHub

- `git init` na raiz do projeto.
- `.gitignore` padrão Java/Maven (`target/`, `*.class`, `*.db`, arquivos de
  IDE).
- Commit inicial com a estrutura do projeto.
- Criação do repositório `poo-jdbc-estudos` (privado) na conta pessoal
  `Matheus904-12` via `gh repo create`.
- Push da branch principal (`main`) para o `origin`.
