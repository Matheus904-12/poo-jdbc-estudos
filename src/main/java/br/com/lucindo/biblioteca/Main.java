package br.com.lucindo.biblioteca;

import br.com.lucindo.biblioteca.config.ConexaoFactory;
import br.com.lucindo.biblioteca.dao.EmprestimoDAO;
import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.dao.UsuarioDAO;
import br.com.lucindo.biblioteca.dao.sqlite.EmprestimoDAOSQLite;
import br.com.lucindo.biblioteca.dao.sqlite.LivroDAOSQLite;
import br.com.lucindo.biblioteca.dao.sqlite.UsuarioDAOSQLite;
import br.com.lucindo.biblioteca.model.Emprestimo;
import br.com.lucindo.biblioteca.model.Livro;
import br.com.lucindo.biblioteca.model.Usuario;
import br.com.lucindo.biblioteca.service.EmprestimoService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (var conexaoInicial = ConexaoFactory.obterConexao()) {
            System.out.println("Sistema iniciado. Banco de dados pronto.");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar o banco de dados: " + e.getMessage());
            return;
        }

        LivroDAO livroDAO = new LivroDAOSQLite();
        UsuarioDAO usuarioDAO = new UsuarioDAOSQLite();
        EmprestimoDAO emprestimoDAO = new EmprestimoDAOSQLite();
        EmprestimoService emprestimoService = new EmprestimoService(livroDAO, usuarioDAO, emprestimoDAO);

        try (Scanner scanner = new Scanner(System.in)) {
            int opcao;
            do {
                exibirMenu();
                opcao = lerInteiro(scanner);
                switch (opcao) {
                    case 1 -> cadastrarLivro(scanner, livroDAO);
                    case 2 -> cadastrarUsuario(scanner, usuarioDAO);
                    case 3 -> registrarEmprestimo(scanner, emprestimoService);
                    case 4 -> registrarDevolucao(scanner, emprestimoService);
                    case 5 -> listarLivrosDisponiveis(livroDAO);
                    case 6 -> listarEmprestimosAtivos(emprestimoDAO);
                    case 0 -> System.out.println("Ate mais!");
                    default -> System.out.println("Opcao invalida.");
                }
            } while (opcao != 0);
        }
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("=== Biblioteca ===");
        System.out.println("1. Cadastrar livro");
        System.out.println("2. Cadastrar usuario");
        System.out.println("3. Registrar emprestimo");
        System.out.println("4. Registrar devolucao");
        System.out.println("5. Listar livros disponiveis");
        System.out.println("6. Listar emprestimos ativos");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opcao: ");
    }

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
