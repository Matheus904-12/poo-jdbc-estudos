package br.com.lucindo.biblioteca;

import br.com.lucindo.biblioteca.config.ConexaoFactory;
import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.dao.sqlite.LivroDAOSQLite;
import br.com.lucindo.biblioteca.model.Livro;

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

        try (Scanner scanner = new Scanner(System.in)) {
            int opcao;
            do {
                exibirMenu();
                opcao = lerInteiro(scanner);
                switch (opcao) {
                    case 1 -> cadastrarLivro(scanner, livroDAO);
                    case 5 -> listarLivrosDisponiveis(livroDAO);
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
        System.out.println("5. Listar livros disponiveis");
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
