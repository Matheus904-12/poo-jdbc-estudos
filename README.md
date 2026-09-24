# Sistema de Biblioteca — POO + JDBC

Projeto de estudo em Java: Programacao Orientada a Objetos + acesso a
banco de dados via JDBC (SQLite), sem frameworks.

## Arquitetura

- `model`: classes de dominio (Livro, Usuario, Emprestimo).
- `dao`: interfaces + implementacoes SQLite (padrao DAO), isolando o SQL
  do resto da aplicacao.
- `service`: `EmprestimoService` concentra as regras de negocio
  (prazo de 14 dias, multa por atraso, transacao JDBC na
  devolucao/emprestimo).
- `Main`: menu de console que orquestra tudo.

## Requisitos

- Java 21 (`java -version`)
- Nenhuma instalacao adicional de Maven: o projeto usa um wrapper local
  `./mvn` que aponta para o Maven empacotado com o NetBeans.

## Como rodar

```bash
./mvn compile exec:java
```

Na primeira execucao, o arquivo `biblioteca.db` (SQLite) e criado
automaticamente na raiz do projeto, com as tabelas ja configuradas.

## Funcionalidades

1. Cadastrar livro
2. Cadastrar usuario
3. Registrar emprestimo (recusa se nao houver exemplar disponivel)
4. Registrar devolucao (calcula multa de R$ 2,00/dia de atraso)
5. Listar livros disponiveis
6. Listar emprestimos ativos

## O que este projeto pratica

- Encapsulamento e construtores em classes de dominio.
- Interfaces + implementacoes concretas (padrao DAO / polimorfismo).
- `PreparedStatement` para evitar SQL injection.
- Transacoes JDBC (`Connection.setAutoCommit(false)` + commit/rollback)
  quando mais de uma tabela precisa mudar de forma atomica.
- Tratamento de excecoes: encapsular `SQLException` (checked) em uma
  excecao de aplicacao (`RepositorioException`, unchecked).
