# Sistema de Biblioteca (POO + JDBC) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir um sistema de biblioteca em console (Java 21 + JDBC + SQLite) para praticar POO — DAO com interfaces, camada de serviço com regras de negócio e transações JDBC — e publicá-lo num repositório GitHub pessoal.

**Architecture:** Três camadas: `model` (POJOs), `dao` (interface + implementação SQLite por entidade, recebendo `Connection` como parâmetro para permitir transações cross-DAO controladas pelo chamador), `service` (`EmprestimoService` concentra regras de negócio de empréstimo/devolução, incluindo a transação JDBC). `Main` é um menu de console que orquestra tudo.

**Tech Stack:** Java 21, Maven (via binário empacotado com o NetBeans, sem instalação adicional — ver Global Constraints), driver `org.xerial:sqlite-jdbc`, banco SQLite em arquivo (`biblioteca.db`).

**Spec:** `docs/superpowers/specs/2026-09-23-poo-jdbc-estudos-design.md`

## Global Constraints

- Java 21 é a versão alvo (`maven.compiler.release=21`).
- Sem testes automatizados (JUnit) nesta versão — conforme a spec, a verificação de cada tarefa é feita via `mvn compile`, `jshell` para POJOs isolados, e execução do console (`mvn exec:java`) com entrada simulada via `printf | ...`.
- Maven não está no PATH do sistema. Existe um binário funcional empacotado com o NetBeans em `/snap/netbeans/current/netbeans/java/maven/bin/mvn`. A Tarefa 1 cria um script wrapper `./mvn` na raiz do projeto que aponta pra esse binário — **todos os comandos de build deste plano usam `./mvn`, executado a partir da raiz do projeto**, nunca `mvn` direto.
- Banco de dados: SQLite, arquivo `biblioteca.db` criado automaticamente na raiz do projeto na primeira conexão (via `ConexaoFactory` + `schema.sql`). O arquivo é ignorado pelo git (`.gitignore`).
- Todo método DAO que participa de escrita ou pode ser combinado em transação recebe `java.sql.Connection` como parâmetro explícito — quem abre/fecha a conexão e controla commit/rollback é sempre o chamador (`Main` para operações simples, `EmprestimoService` para as que exigem transação).
- Textos de UI (menu, mensagens de erro) em português, sem acentos problemáticos em `System.out` (usar apenas caracteres ASCII simples pra evitar problemas de encoding no terminal).

## Review Focus

- Cadastro de livro com ISBN já existente (constraint `UNIQUE` do SQLite) deve mostrar mensagem de erro amigável e manter o app rodando, nunca um stack trace cru ou encerramento abrupto — testado na Tarefa 4.
- Cadastro de usuário com email já existente (constraint `UNIQUE`) tem o mesmo requisito acima — testado na Tarefa 5.
- Tentar registrar empréstimo de um livro sem exemplares disponíveis (`quantidade_disponivel == 0`) deve ser recusado com mensagem clara, sem alterar o banco — testado na Tarefa 6.
- Tentar registrar devolução de um empréstimo já devolvido (`status == DEVOLVIDO`) deve ser recusado com mensagem clara, sem alterar o banco — testado na Tarefa 6.
- Tentar registrar empréstimo com um ID de usuário inexistente: o SQLite não aplica a constraint `FOREIGN KEY` por padrão (sem `PRAGMA foreign_keys = ON`), então sem validação explícita o registro seria inserido silenciosamente com um `usuario_id` órfão — testado na Tarefa 6.
- Entrada não numérica em campos que esperam número no menu (ex.: digitar texto onde se espera o ID) não pode travar o programa em loop de exceção nem derrubá-lo com `InputMismatchException` — testado na Tarefa 1, através de `lerInteiro`, reutilizado por todas as telas que pedem número.

---

### Task 1: Esqueleto do projeto Maven + menu de console inicial

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `mvn` (script wrapper executável)
- Create: `README.md`
- Create: `src/main/java/br/com/lucindo/biblioteca/Main.java`

**Interfaces:**
- Consumes: nada (primeira tarefa).
- Produces: `Main.main(String[])` executável via `./mvn exec:java`; método privado `Main.lerInteiro(Scanner)` — assinatura `private static int lerInteiro(Scanner scanner)` — reutilizado pelas Tarefas 4, 5 e 6.

- [ ] **Step 1: Criar `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.lucindo</groupId>
    <artifactId>poo-jdbc-estudos</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.46.1.3</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <mainClass>br.com.lucindo.biblioteca.Main</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Criar `.gitignore`**

```
target/
*.class
*.db
.idea/
*.iml
.vscode/
```

- [ ] **Step 3: Criar o script wrapper `mvn`**

```bash
#!/bin/bash
exec /snap/netbeans/current/netbeans/java/maven/bin/mvn "$@"
```

Rodar: `chmod +x mvn`

- [ ] **Step 4: Criar `README.md` (esqueleto)**

```markdown
# Sistema de Biblioteca — POO + JDBC

Projeto de estudo em Java: Programacao Orientada a Objetos + acesso a
banco de dados via JDBC (SQLite), sem frameworks.

## Como rodar

./mvn compile exec:java
```

- [ ] **Step 5: Criar `Main.java`**

```java
package br.com.lucindo.biblioteca;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Sistema iniciado.");

        try (Scanner scanner = new Scanner(System.in)) {
            int opcao;
            do {
                exibirMenu();
                opcao = lerInteiro(scanner);
                switch (opcao) {
                    case 0 -> System.out.println("Ate mais!");
                    default -> System.out.println("Opcao invalida.");
                }
            } while (opcao != 0);
        }
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("=== Biblioteca ===");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opcao: ");
    }

    private static int lerInteiro(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Digite um numero valido: ");
            scanner.next();
        }
        int valor = scanner.nextInt();
        scanner.nextLine();
        return valor;
    }
}
```

- [ ] **Step 6: Compilar e rodar com entrada valida**

Run: `./mvn -q compile exec:java <<< $'0\n'`
Expected: a saída contém `Sistema iniciado.` e `Ate mais!`

- [ ] **Step 7: Testar entrada nao numerica (Review Focus: robustez do menu)**

Run: `printf 'abc\n0\n' | ./mvn -q compile exec:java`
Expected: a saída contém `Digite um numero valido:` (não trava, não lança stack trace) e termina com `Ate mais!`

- [ ] **Step 8: Commit**

```bash
git add pom.xml .gitignore mvn README.md src/
git commit -m "feat: esqueleto do projeto Maven com menu de console inicial"
```

---

### Task 2: Classes de modelo (Livro, Usuario, Emprestimo)

**Files:**
- Create: `src/main/java/br/com/lucindo/biblioteca/model/Livro.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/model/Usuario.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/model/Emprestimo.java`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `Livro(String titulo, String autor, String isbn, int quantidadeTotal)` — quantidadeDisponivel inicial = quantidadeTotal.
  - `Livro(Integer id, String titulo, String autor, String isbn, int quantidadeTotal, int quantidadeDisponivel)`.
  - Getters/setters: `getId/setId`, `getTitulo/setTitulo`, `getAutor/setAutor`, `getIsbn/setIsbn`, `getQuantidadeTotal/setQuantidadeTotal`, `getQuantidadeDisponivel/setQuantidadeDisponivel`.
  - `Usuario(String nome, String email)`, `Usuario(Integer id, String nome, String email)`, com getters/setters `getId/setId`, `getNome/setNome`, `getEmail/setEmail`.
  - `Emprestimo.Status` enum (`ATIVO`, `DEVOLVIDO`).
  - `Emprestimo(int livroId, int usuarioId, LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao)` — status inicial `ATIVO`.
  - `Emprestimo(Integer id, int livroId, int usuarioId, LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao, LocalDate dataRealDevolucao, Status status)`.
  - Getters: `getId/setId`, `getLivroId`, `getUsuarioId`, `getDataEmprestimo`, `getDataPrevistaDevolucao`, `getDataRealDevolucao/setDataRealDevolucao`, `getStatus/setStatus`.

- [ ] **Step 1: Criar `Livro.java`**

```java
package br.com.lucindo.biblioteca.model;

public class Livro {
    private Integer id;
    private String titulo;
    private String autor;
    private String isbn;
    private int quantidadeTotal;
    private int quantidadeDisponivel;

    public Livro(String titulo, String autor, String isbn, int quantidadeTotal) {
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeDisponivel = quantidadeTotal;
    }

    public Livro(Integer id, String titulo, String autor, String isbn,
                 int quantidadeTotal, int quantidadeDisponivel) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getQuantidadeTotal() { return quantidadeTotal; }
    public void setQuantidadeTotal(int quantidadeTotal) { this.quantidadeTotal = quantidadeTotal; }
    public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public void setQuantidadeDisponivel(int quantidadeDisponivel) { this.quantidadeDisponivel = quantidadeDisponivel; }

    @Override
    public String toString() {
        return String.format("#%d | %s - %s (ISBN: %s) | Disponiveis: %d/%d",
                id, titulo, autor, isbn, quantidadeDisponivel, quantidadeTotal);
    }
}
```

- [ ] **Step 2: Criar `Usuario.java`**

```java
package br.com.lucindo.biblioteca.model;

public class Usuario {
    private Integer id;
    private String nome;
    private String email;

    public Usuario(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public Usuario(Integer id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return String.format("#%d | %s <%s>", id, nome, email);
    }
}
```

- [ ] **Step 3: Criar `Emprestimo.java`**

```java
package br.com.lucindo.biblioteca.model;

import java.time.LocalDate;

public class Emprestimo {
    public enum Status { ATIVO, DEVOLVIDO }

    private Integer id;
    private int livroId;
    private int usuarioId;
    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataRealDevolucao;
    private Status status;

    public Emprestimo(int livroId, int usuarioId, LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao) {
        this.livroId = livroId;
        this.usuarioId = usuarioId;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
        this.status = Status.ATIVO;
    }

    public Emprestimo(Integer id, int livroId, int usuarioId, LocalDate dataEmprestimo,
                       LocalDate dataPrevistaDevolucao, LocalDate dataRealDevolucao, Status status) {
        this.id = id;
        this.livroId = livroId;
        this.usuarioId = usuarioId;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
        this.dataRealDevolucao = dataRealDevolucao;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getLivroId() { return livroId; }
    public int getUsuarioId() { return usuarioId; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public LocalDate getDataRealDevolucao() { return dataRealDevolucao; }
    public void setDataRealDevolucao(LocalDate dataRealDevolucao) { this.dataRealDevolucao = dataRealDevolucao; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("#%d | Livro #%d -> Usuario #%d | Emprestado em %s | Previsto: %s | Status: %s",
                id, livroId, usuarioId, dataEmprestimo, dataPrevistaDevolucao, status);
    }
}
```

- [ ] **Step 4: Compilar**

Run: `./mvn -q compile`
Expected: build sem erros (sem saída = sucesso).

- [ ] **Step 5: Verificar comportamento do construtor de Livro via jshell**

Run:
```bash
echo 'import br.com.lucindo.biblioteca.model.Livro; Livro l = new Livro("Dom Casmurro","Machado de Assis","123",5); System.out.println(l.getQuantidadeDisponivel());' | jshell --class-path target/classes -q -
```
Expected: a saída contém `5` (quantidadeDisponivel inicializada igual a quantidadeTotal).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/lucindo/biblioteca/model/
git commit -m "feat: adiciona classes de modelo Livro, Usuario e Emprestimo"
```

---

### Task 3: Conexao JDBC + schema do banco

**Files:**
- Create: `src/main/java/br/com/lucindo/biblioteca/exception/RepositorioException.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/config/ConexaoFactory.java`
- Create: `src/main/resources/schema.sql`
- Modify: `src/main/java/br/com/lucindo/biblioteca/Main.java`

**Interfaces:**
- Consumes: nenhum tipo das Tarefas anteriores diretamente.
- Produces:
  - `RepositorioException extends RuntimeException`, construtor `RepositorioException(String mensagem, Throwable causa)`.
  - `ConexaoFactory.obterConexao()` — assinatura `public static Connection obterConexao()` — usada por todas as DAOs e pelo `Main` a partir daqui.

- [ ] **Step 1: Criar `RepositorioException.java`**

```java
package br.com.lucindo.biblioteca.exception;

public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
```

- [ ] **Step 2: Criar `schema.sql`**

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
    status TEXT NOT NULL,
    FOREIGN KEY (livro_id) REFERENCES livro(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
```

- [ ] **Step 3: Criar `ConexaoFactory.java`**

```java
package br.com.lucindo.biblioteca.config;

import br.com.lucindo.biblioteca.exception.RepositorioException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoFactory {
    private static final String URL = "jdbc:sqlite:biblioteca.db";
    private static boolean schemaInicializado = false;

    public static Connection obterConexao() {
        try {
            Connection conexao = DriverManager.getConnection(URL);
            if (!schemaInicializado) {
                inicializarSchema(conexao);
                schemaInicializado = true;
            }
            return conexao;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao conectar ao banco SQLite", e);
        }
    }

    private static void inicializarSchema(Connection conexao) {
        try (InputStream entrada = ConexaoFactory.class.getResourceAsStream("/schema.sql")) {
            if (entrada == null) {
                throw new IllegalStateException("Arquivo schema.sql nao encontrado no classpath");
            }
            String script = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = conexao.createStatement()) {
                for (String comando : script.split(";")) {
                    if (!comando.isBlank()) {
                        statement.execute(comando);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RepositorioException("Falha ao inicializar o schema do banco", e);
        }
    }
}
```

- [ ] **Step 4: Atualizar `Main.java` para inicializar a conexao no startup**

Adicionar o import no topo do arquivo, logo apos `package br.com.lucindo.biblioteca;`:
```java
import br.com.lucindo.biblioteca.config.ConexaoFactory;
```

Substituir a primeira linha do `main` (`System.out.println("Sistema iniciado.");`) por:

```java
        try (var conexaoInicial = ConexaoFactory.obterConexao()) {
            System.out.println("Sistema iniciado. Banco de dados pronto.");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar o banco de dados: " + e.getMessage());
            return;
        }
```

- [ ] **Step 5: Rodar e verificar criacao do banco**

Run: `rm -f biblioteca.db && ./mvn -q compile exec:java <<< $'0\n' && ls -la biblioteca.db`
Expected: saída contém `Sistema iniciado. Banco de dados pronto.` e o comando `ls` lista `biblioteca.db` (arquivo criado).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/lucindo/biblioteca/exception/ src/main/java/br/com/lucindo/biblioteca/config/ src/main/resources/schema.sql src/main/java/br/com/lucindo/biblioteca/Main.java
git commit -m "feat: adiciona ConexaoFactory e schema inicial do banco SQLite"
```

---

### Task 4: LivroDAO + telas de cadastro/listagem de livros

**Files:**
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/LivroDAO.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/sqlite/LivroDAOSQLite.java`
- Modify: `src/main/java/br/com/lucindo/biblioteca/Main.java`

**Interfaces:**
- Consumes: `Livro` (Task 2), `ConexaoFactory.obterConexao()` e `RepositorioException` (Task 3).
- Produces: `LivroDAO` com `void salvar(Connection, Livro)`, `Livro buscarPorId(Connection, int)`, `List<Livro> listarDisponiveis(Connection)`, `void atualizar(Connection, Livro)` — implementado por `LivroDAOSQLite`, usado pelas Tasks 6 e 7.

- [ ] **Step 1: Criar a interface `LivroDAO.java`**

```java
package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Livro;

import java.sql.Connection;
import java.util.List;

public interface LivroDAO {
    void salvar(Connection conexao, Livro livro);
    Livro buscarPorId(Connection conexao, int id);
    List<Livro> listarDisponiveis(Connection conexao);
    void atualizar(Connection conexao, Livro livro);
}
```

- [ ] **Step 2: Criar `LivroDAOSQLite.java`**

```java
package br.com.lucindo.biblioteca.dao.sqlite;

import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.exception.RepositorioException;
import br.com.lucindo.biblioteca.model.Livro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LivroDAOSQLite implements LivroDAO {

    @Override
    public void salvar(Connection conexao, Livro livro) {
        String sql = "INSERT INTO livro (titulo, autor, isbn, quantidade_total, quantidade_disponivel) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, livro.getTitulo());
            statement.setString(2, livro.getAutor());
            statement.setString(3, livro.getIsbn());
            statement.setInt(4, livro.getQuantidadeTotal());
            statement.setInt(5, livro.getQuantidadeDisponivel());
            statement.executeUpdate();

            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    livro.setId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar livro", e);
        }
    }

    @Override
    public Livro buscarPorId(Connection conexao, int id) {
        String sql = "SELECT * FROM livro WHERE id = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar livro por id", e);
        }
    }

    @Override
    public List<Livro> listarDisponiveis(Connection conexao) {
        String sql = "SELECT * FROM livro WHERE quantidade_disponivel > 0 ORDER BY titulo";
        List<Livro> livros = new ArrayList<>();
        try (PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                livros.add(mapear(resultado));
            }
            return livros;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar livros disponiveis", e);
        }
    }

    @Override
    public void atualizar(Connection conexao, Livro livro) {
        String sql = "UPDATE livro SET titulo = ?, autor = ?, isbn = ?, quantidade_total = ?, quantidade_disponivel = ? WHERE id = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, livro.getTitulo());
            statement.setString(2, livro.getAutor());
            statement.setString(3, livro.getIsbn());
            statement.setInt(4, livro.getQuantidadeTotal());
            statement.setInt(5, livro.getQuantidadeDisponivel());
            statement.setInt(6, livro.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao atualizar livro", e);
        }
    }

    private Livro mapear(ResultSet resultado) throws SQLException {
        return new Livro(
                resultado.getInt("id"),
                resultado.getString("titulo"),
                resultado.getString("autor"),
                resultado.getString("isbn"),
                resultado.getInt("quantidade_total"),
                resultado.getInt("quantidade_disponivel")
        );
    }
}
```

- [ ] **Step 3: Atualizar `Main.java`**

Adicionar imports (o import de `ConexaoFactory` já foi adicionado na Tarefa 3):
```java
import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.dao.sqlite.LivroDAOSQLite;
import br.com.lucindo.biblioteca.model.Livro;
import java.sql.Connection;
import java.util.List;
```

No inicio do `main`, apos a inicializacao do banco (Step 4 da Task 3), instanciar a DAO:
```java
        LivroDAO livroDAO = new LivroDAOSQLite();
```
(essa variavel precisa estar visivel no escopo do `try` com o `Scanner`, entao declare-a antes do bloco `try (Scanner scanner = ...)`)

Atualizar `exibirMenu()`:
```java
    private static void exibirMenu() {
        System.out.println();
        System.out.println("=== Biblioteca ===");
        System.out.println("1. Cadastrar livro");
        System.out.println("5. Listar livros disponiveis");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opcao: ");
    }
```

Atualizar o `switch` dentro do `do/while`:
```java
                switch (opcao) {
                    case 1 -> cadastrarLivro(scanner, livroDAO);
                    case 5 -> listarLivrosDisponiveis(livroDAO);
                    case 0 -> System.out.println("Ate mais!");
                    default -> System.out.println("Opcao invalida.");
                }
```

Adicionar os metodos novos (no fim da classe, antes da ultima chave):
```java
    private static void cadastrarLivro(Scanner scanner, LivroDAO livroDAO) {
        System.out.print("Titulo: ");
        String titulo = scanner.nextLine();
        System.out.print("Autor: ");
        String autor = scanner.nextLine();
        System.out.print("ISBN: ");
        String isbn = scanner.nextLine();
        System.out.print("Quantidade de exemplares: ");
        int quantidade = lerInteiro(scanner);

        Livro livro = new Livro(titulo, autor, isbn, quantidade);
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            livroDAO.salvar(conexao, livro);
            System.out.println("Livro cadastrado: " + livro);
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar livro: " + e.getMessage());
        }
    }

    private static void listarLivrosDisponiveis(LivroDAO livroDAO) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            List<Livro> livros = livroDAO.listarDisponiveis(conexao);
            if (livros.isEmpty()) {
                System.out.println("Nenhum livro disponivel.");
            }
            livros.forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Erro ao listar livros: " + e.getMessage());
        }
    }
```

- [ ] **Step 4: Testar cadastro e listagem**

Run: `rm -f biblioteca.db && printf '1\nDom Casmurro\nMachado de Assis\nISBN-001\n3\n5\n0\n' | ./mvn -q compile exec:java`
Expected: a saída contém `Livro cadastrado:` e, na listagem (opção 5), a linha `Dom Casmurro - Machado de Assis (ISBN: ISBN-001) | Disponiveis: 3/3`.

- [ ] **Step 5: Testar ISBN duplicado (Review Focus)**

Run: `printf '1\nDom Casmurro\nMachado de Assis\nISBN-001\n2\n0\n' | ./mvn -q exec:java`
Expected: a saída contém `Erro ao cadastrar livro:` (mensagem de erro amigável, sem stack trace, programa continua e termina normalmente com `Ate mais!`).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/lucindo/biblioteca/dao/ src/main/java/br/com/lucindo/biblioteca/Main.java
git commit -m "feat: adiciona LivroDAO e telas de cadastro/listagem de livros"
```

---

### Task 5: UsuarioDAO + tela de cadastro de usuario

**Files:**
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/UsuarioDAO.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/sqlite/UsuarioDAOSQLite.java`
- Modify: `src/main/java/br/com/lucindo/biblioteca/Main.java`

**Interfaces:**
- Consumes: `Usuario` (Task 2), `ConexaoFactory.obterConexao()` e `RepositorioException` (Task 3).
- Produces: `UsuarioDAO` com `void salvar(Connection, Usuario)`, `Usuario buscarPorId(Connection, int)`, `List<Usuario> listarTodos(Connection)` — implementado por `UsuarioDAOSQLite`, usado pela Task 6.

- [ ] **Step 1: Criar a interface `UsuarioDAO.java`**

```java
package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Usuario;

import java.sql.Connection;
import java.util.List;

public interface UsuarioDAO {
    void salvar(Connection conexao, Usuario usuario);
    Usuario buscarPorId(Connection conexao, int id);
    List<Usuario> listarTodos(Connection conexao);
}
```

- [ ] **Step 2: Criar `UsuarioDAOSQLite.java`**

```java
package br.com.lucindo.biblioteca.dao.sqlite;

import br.com.lucindo.biblioteca.dao.UsuarioDAO;
import br.com.lucindo.biblioteca.exception.RepositorioException;
import br.com.lucindo.biblioteca.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOSQLite implements UsuarioDAO {

    @Override
    public void salvar(Connection conexao, Usuario usuario) {
        String sql = "INSERT INTO usuario (nome, email) VALUES (?, ?)";
        try (PreparedStatement statement = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, usuario.getNome());
            statement.setString(2, usuario.getEmail());
            statement.executeUpdate();

            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    usuario.setId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar usuario", e);
        }
    }

    @Override
    public Usuario buscarPorId(Connection conexao, int id) {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar usuario por id", e);
        }
    }

    @Override
    public List<Usuario> listarTodos(Connection conexao) {
        String sql = "SELECT * FROM usuario ORDER BY nome";
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                usuarios.add(mapear(resultado));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar usuarios", e);
        }
    }

    private Usuario mapear(ResultSet resultado) throws SQLException {
        return new Usuario(
                resultado.getInt("id"),
                resultado.getString("nome"),
                resultado.getString("email")
        );
    }
}
```

- [ ] **Step 3: Atualizar `Main.java`**

Adicionar imports:
```java
import br.com.lucindo.biblioteca.dao.UsuarioDAO;
import br.com.lucindo.biblioteca.dao.sqlite.UsuarioDAOSQLite;
import br.com.lucindo.biblioteca.model.Usuario;
```

Instanciar junto com `livroDAO`:
```java
        UsuarioDAO usuarioDAO = new UsuarioDAOSQLite();
```

Atualizar `exibirMenu()` para incluir:
```java
        System.out.println("2. Cadastrar usuario");
```
(logo apos a linha `1. Cadastrar livro`)

Atualizar o `switch`:
```java
                    case 2 -> cadastrarUsuario(scanner, usuarioDAO);
```
(logo apos o `case 1`)

Adicionar o metodo novo:
```java
    private static void cadastrarUsuario(Scanner scanner, UsuarioDAO usuarioDAO) {
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();

        Usuario usuario = new Usuario(nome, email);
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            usuarioDAO.salvar(conexao, usuario);
            System.out.println("Usuario cadastrado: " + usuario);
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar usuario: " + e.getMessage());
        }
    }
```

- [ ] **Step 4: Testar cadastro de usuario**

Run: `printf '2\nJoao Silva\njoao@example.com\n0\n' | ./mvn -q compile exec:java`
Expected: a saída contém `Usuario cadastrado: #1 | Joao Silva <joao@example.com>` (o número do ID pode variar conforme execuções anteriores do banco).

- [ ] **Step 5: Testar email duplicado (Review Focus)**

Run: `printf '2\nJoao Silva\njoao@example.com\n0\n' | ./mvn -q exec:java`
Expected: a saída contém `Erro ao cadastrar usuario:` (mensagem amigável, programa continua e termina com `Ate mais!`).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/lucindo/biblioteca/dao/ src/main/java/br/com/lucindo/biblioteca/Main.java
git commit -m "feat: adiciona UsuarioDAO e tela de cadastro de usuario"
```

---

### Task 6: EmprestimoDAO + EmprestimoService + telas de emprestimo/devolucao

**Files:**
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/EmprestimoDAO.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/dao/sqlite/EmprestimoDAOSQLite.java`
- Create: `src/main/java/br/com/lucindo/biblioteca/service/EmprestimoService.java`
- Modify: `src/main/java/br/com/lucindo/biblioteca/Main.java`

**Interfaces:**
- Consumes: `Emprestimo`, `Livro` (Task 2); `LivroDAO` (Task 4); `UsuarioDAO` (Task 5); `ConexaoFactory.obterConexao()`, `RepositorioException` (Task 3).
- Produces:
  - `EmprestimoDAO` com `void salvar(Connection, Emprestimo)`, `Emprestimo buscarPorId(Connection, int)`, `List<Emprestimo> listarAtivos(Connection)`, `void atualizar(Connection, Emprestimo)`.
  - `EmprestimoService(LivroDAO livroDAO, UsuarioDAO usuarioDAO, EmprestimoDAO emprestimoDAO)`, `Emprestimo registrarEmprestimo(int livroId, int usuarioId)` (lança `IllegalStateException` se livro ou usuário inexistente, ou se não houver exemplares), `BigDecimal registrarDevolucao(int emprestimoId)` (lança `IllegalStateException` se empréstimo inexistente ou já devolvido; retorna o valor da multa, `BigDecimal.ZERO` se não houver atraso).

- [ ] **Step 1: Criar a interface `EmprestimoDAO.java`**

```java
package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Emprestimo;

import java.sql.Connection;
import java.util.List;

public interface EmprestimoDAO {
    void salvar(Connection conexao, Emprestimo emprestimo);
    Emprestimo buscarPorId(Connection conexao, int id);
    List<Emprestimo> listarAtivos(Connection conexao);
    void atualizar(Connection conexao, Emprestimo emprestimo);
}
```

- [ ] **Step 2: Criar `EmprestimoDAOSQLite.java`**

```java
package br.com.lucindo.biblioteca.dao.sqlite;

import br.com.lucindo.biblioteca.dao.EmprestimoDAO;
import br.com.lucindo.biblioteca.exception.RepositorioException;
import br.com.lucindo.biblioteca.model.Emprestimo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmprestimoDAOSQLite implements EmprestimoDAO {

    @Override
    public void salvar(Connection conexao, Emprestimo emprestimo) {
        String sql = "INSERT INTO emprestimo (livro_id, usuario_id, data_emprestimo, data_prevista_devolucao, data_real_devolucao, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, emprestimo.getLivroId());
            statement.setInt(2, emprestimo.getUsuarioId());
            statement.setString(3, emprestimo.getDataEmprestimo().toString());
            statement.setString(4, emprestimo.getDataPrevistaDevolucao().toString());
            statement.setString(5, emprestimo.getDataRealDevolucao() == null ? null : emprestimo.getDataRealDevolucao().toString());
            statement.setString(6, emprestimo.getStatus().name());
            statement.executeUpdate();

            try (ResultSet chaves = statement.getGeneratedKeys()) {
                if (chaves.next()) {
                    emprestimo.setId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar emprestimo", e);
        }
    }

    @Override
    public Emprestimo buscarPorId(Connection conexao, int id) {
        String sql = "SELECT * FROM emprestimo WHERE id = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar emprestimo por id", e);
        }
    }

    @Override
    public List<Emprestimo> listarAtivos(Connection conexao) {
        String sql = "SELECT * FROM emprestimo WHERE status = 'ATIVO' ORDER BY data_prevista_devolucao";
        List<Emprestimo> emprestimos = new ArrayList<>();
        try (PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultado = statement.executeQuery()) {
            while (resultado.next()) {
                emprestimos.add(mapear(resultado));
            }
            return emprestimos;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar emprestimos ativos", e);
        }
    }

    @Override
    public void atualizar(Connection conexao, Emprestimo emprestimo) {
        String sql = "UPDATE emprestimo SET data_real_devolucao = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, emprestimo.getDataRealDevolucao() == null ? null : emprestimo.getDataRealDevolucao().toString());
            statement.setString(2, emprestimo.getStatus().name());
            statement.setInt(3, emprestimo.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao atualizar emprestimo", e);
        }
    }

    private Emprestimo mapear(ResultSet resultado) throws SQLException {
        String dataRealTexto = resultado.getString("data_real_devolucao");
        return new Emprestimo(
                resultado.getInt("id"),
                resultado.getInt("livro_id"),
                resultado.getInt("usuario_id"),
                LocalDate.parse(resultado.getString("data_emprestimo")),
                LocalDate.parse(resultado.getString("data_prevista_devolucao")),
                dataRealTexto == null ? null : LocalDate.parse(dataRealTexto),
                Emprestimo.Status.valueOf(resultado.getString("status"))
        );
    }
}
```

- [ ] **Step 3: Criar `EmprestimoService.java`**

```java
package br.com.lucindo.biblioteca.service;

import br.com.lucindo.biblioteca.config.ConexaoFactory;
import br.com.lucindo.biblioteca.dao.EmprestimoDAO;
import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.dao.UsuarioDAO;
import br.com.lucindo.biblioteca.exception.RepositorioException;
import br.com.lucindo.biblioteca.model.Emprestimo;
import br.com.lucindo.biblioteca.model.Livro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EmprestimoService {
    private static final int PRAZO_DIAS = 14;
    private static final BigDecimal MULTA_POR_DIA = new BigDecimal("2.00");

    private final LivroDAO livroDAO;
    private final UsuarioDAO usuarioDAO;
    private final EmprestimoDAO emprestimoDAO;

    public EmprestimoService(LivroDAO livroDAO, UsuarioDAO usuarioDAO, EmprestimoDAO emprestimoDAO) {
        this.livroDAO = livroDAO;
        this.usuarioDAO = usuarioDAO;
        this.emprestimoDAO = emprestimoDAO;
    }

    public Emprestimo registrarEmprestimo(int livroId, int usuarioId) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            Livro livro = livroDAO.buscarPorId(conexao, livroId);
            if (livro == null) {
                throw new IllegalStateException("Livro nao encontrado: " + livroId);
            }
            if (usuarioDAO.buscarPorId(conexao, usuarioId) == null) {
                throw new IllegalStateException("Usuario nao encontrado: " + usuarioId);
            }
            if (livro.getQuantidadeDisponivel() <= 0) {
                throw new IllegalStateException("Nao ha exemplares disponiveis para o livro: " + livro.getTitulo());
            }

            LocalDate hoje = LocalDate.now();
            Emprestimo emprestimo = new Emprestimo(livroId, usuarioId, hoje, hoje.plusDays(PRAZO_DIAS));
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);

            conexao.setAutoCommit(false);
            try {
                emprestimoDAO.salvar(conexao, emprestimo);
                livroDAO.atualizar(conexao, livro);
                conexao.commit();
            } catch (RuntimeException e) {
                conexao.rollback();
                throw e;
            }

            return emprestimo;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar emprestimo", e);
        }
    }

    public BigDecimal registrarDevolucao(int emprestimoId) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            Emprestimo emprestimo = emprestimoDAO.buscarPorId(conexao, emprestimoId);
            if (emprestimo == null) {
                throw new IllegalStateException("Emprestimo nao encontrado: " + emprestimoId);
            }
            if (emprestimo.getStatus() == Emprestimo.Status.DEVOLVIDO) {
                throw new IllegalStateException("Emprestimo ja foi devolvido: " + emprestimoId);
            }

            Livro livro = livroDAO.buscarPorId(conexao, emprestimo.getLivroId());
            LocalDate hoje = LocalDate.now();
            BigDecimal multa = calcularMulta(emprestimo.getDataPrevistaDevolucao(), hoje);

            emprestimo.setDataRealDevolucao(hoje);
            emprestimo.setStatus(Emprestimo.Status.DEVOLVIDO);
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);

            conexao.setAutoCommit(false);
            try {
                emprestimoDAO.atualizar(conexao, emprestimo);
                livroDAO.atualizar(conexao, livro);
                conexao.commit();
            } catch (RuntimeException e) {
                conexao.rollback();
                throw e;
            }

            return multa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar devolucao", e);
        }
    }

    private BigDecimal calcularMulta(LocalDate dataPrevista, LocalDate dataReal) {
        long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, dataReal);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO;
        }
        return MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso));
    }
}
```

- [ ] **Step 4: Atualizar `Main.java`**

Adicionar imports:
```java
import br.com.lucindo.biblioteca.dao.EmprestimoDAO;
import br.com.lucindo.biblioteca.dao.sqlite.EmprestimoDAOSQLite;
import br.com.lucindo.biblioteca.model.Emprestimo;
import br.com.lucindo.biblioteca.service.EmprestimoService;
import java.math.BigDecimal;
```

Instanciar junto com as outras DAOs:
```java
        EmprestimoDAO emprestimoDAO = new EmprestimoDAOSQLite();
        EmprestimoService emprestimoService = new EmprestimoService(livroDAO, usuarioDAO, emprestimoDAO);
```

Atualizar `exibirMenu()` para incluir (apos `2. Cadastrar usuario` e antes de `5. Listar livros disponiveis`):
```java
        System.out.println("3. Registrar emprestimo");
        System.out.println("4. Registrar devolucao");
```
E apos `5. Listar livros disponiveis`:
```java
        System.out.println("6. Listar emprestimos ativos");
```

Atualizar o `switch`:
```java
                    case 3 -> registrarEmprestimo(scanner, emprestimoService);
                    case 4 -> registrarDevolucao(scanner, emprestimoService);
                    case 6 -> listarEmprestimosAtivos(emprestimoDAO);
```
(`case 3` e `case 4` logo apos `case 2`; `case 6` logo apos `case 5`)

Adicionar os metodos novos:
```java
    private static void registrarEmprestimo(Scanner scanner, EmprestimoService emprestimoService) {
        System.out.print("ID do livro: ");
        int livroId = lerInteiro(scanner);
        System.out.print("ID do usuario: ");
        int usuarioId = lerInteiro(scanner);

        try {
            Emprestimo emprestimo = emprestimoService.registrarEmprestimo(livroId, usuarioId);
            System.out.println("Emprestimo registrado: " + emprestimo);
        } catch (IllegalStateException e) {
            System.out.println("Nao foi possivel registrar o emprestimo: " + e.getMessage());
        }
    }

    private static void registrarDevolucao(Scanner scanner, EmprestimoService emprestimoService) {
        System.out.print("ID do emprestimo: ");
        int emprestimoId = lerInteiro(scanner);

        try {
            BigDecimal multa = emprestimoService.registrarDevolucao(emprestimoId);
            if (multa.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("Devolucao registrada. Multa por atraso: R$ " + multa);
            } else {
                System.out.println("Devolucao registrada. Sem multa.");
            }
        } catch (IllegalStateException e) {
            System.out.println("Nao foi possivel registrar a devolucao: " + e.getMessage());
        }
    }

    private static void listarEmprestimosAtivos(EmprestimoDAO emprestimoDAO) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            List<Emprestimo> emprestimos = emprestimoDAO.listarAtivos(conexao);
            if (emprestimos.isEmpty()) {
                System.out.println("Nenhum emprestimo ativo.");
            }
            emprestimos.forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Erro ao listar emprestimos: " + e.getMessage());
        }
    }
```

- [ ] **Step 5: Testar fluxo completo de emprestimo**

Run: `rm -f biblioteca.db && printf '1\nO Cortico\nAluisio Azevedo\nISBN-100\n1\n2\nMaria Souza\nmaria@example.com\n3\n1\n1\n6\n0\n' | ./mvn -q compile exec:java`
Expected: a saída contém `Emprestimo registrado:` e, na listagem de ativos (opção 6), uma linha `Livro #1 -> Usuario #1` com `Status: ATIVO`.

- [ ] **Step 6: Testar recusa por falta de exemplar (Review Focus)**

Run: `printf '3\n1\n1\n0\n' | ./mvn -q exec:java`
Expected: a saída contém `Nao foi possivel registrar o emprestimo: Nao ha exemplares disponiveis para o livro: O Cortico` (o livro cadastrado no Step 5 tinha só 1 exemplar, já emprestado).

- [ ] **Step 7: Testar devolucao e depois devolucao duplicada (Review Focus)**

Run: `printf '4\n1\n4\n1\n0\n' | ./mvn -q exec:java`
Expected: a primeira devolução (empréstimo #1) imprime `Devolucao registrada.` (com ou sem multa, dependendo da data); a segunda tentativa de devolver o mesmo empréstimo imprime `Nao foi possivel registrar a devolucao: Emprestimo ja foi devolvido: 1`.

- [ ] **Step 8: Testar usuario inexistente (Review Focus)**

Run: `printf '1\nCapitaes da Areia\nJorge Amado\nISBN-200\n2\n3\n1\n999\n0\n' | ./mvn -q exec:java`
Expected: a saída contém `Nao foi possivel registrar o emprestimo: Usuario nao encontrado: 999` (o livro foi cadastrado com sucesso, mas o empréstimo é recusado por causa do usuário inválido).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/br/com/lucindo/biblioteca/dao/ src/main/java/br/com/lucindo/biblioteca/service/ src/main/java/br/com/lucindo/biblioteca/Main.java
git commit -m "feat: adiciona EmprestimoDAO, EmprestimoService e fluxo de emprestimo/devolucao"
```

---

### Task 7: README final

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: nenhuma API nova — apenas documenta o que já existe.
- Produces: nada consumido por outras tarefas.

- [ ] **Step 1: Reescrever `README.md`**

```markdown
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
```

- [ ] **Step 2: Verificar que o comando documentado funciona**

Run: `./mvn -q compile exec:java <<< $'0\n'`
Expected: a saída contém `Sistema iniciado. Banco de dados pronto.` e `Ate mais!` (confirma que o comando do README está correto).

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: adiciona instrucoes de uso e arquitetura ao README"
```

---

### Task 8: Publicar no GitHub (conta pessoal)

**Files:**
- Nenhum arquivo de codigo — apenas comandos `gh`/`git`.

**Interfaces:**
- Consumes: repositorio git local com todos os commits das Tarefas 1-7.
- Produces: repositorio remoto `poo-jdbc-estudos` na conta `Matheus904-12`.

- [ ] **Step 1: Confirmar autenticacao ativa na conta pessoal**

Run: `gh auth status`
Expected: a saída contém `Logged in to github.com account Matheus904-12` com `Active account: true`.

- [ ] **Step 2: Confirmar que a arvore de trabalho esta limpa**

Run: `git status`
Expected: `nada a submeter, diretorio de trabalho limpo` (todos os commits das Tarefas 1-7 ja foram feitos).

- [ ] **Step 3: Criar o repositorio remoto privado e configurar o push**

Run: `gh repo create poo-jdbc-estudos --private --source=. --remote=origin --description "Projeto de estudo: POO + JDBC (SQLite) em Java, sem frameworks."`
Expected: saída confirma a criacao do repositorio em `https://github.com/Matheus904-12/poo-jdbc-estudos` e configura o remoto `origin`.

- [ ] **Step 4: Push da branch main**

Run: `git push -u origin main`
Expected: saída confirma o push bem-sucedido da branch `main` para `origin`.

- [ ] **Step 5: Confirmar no GitHub**

Run: `gh repo view poo-jdbc-estudos --web=false`
Expected: a saída mostra a descricao e a visibilidade `private` do repositorio recem-criado.
