package br.com.lucindo.biblioteca;

import br.com.lucindo.biblioteca.config.ConexaoFactory;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (var conexaoInicial = ConexaoFactory.obterConexao()) {
            System.out.println("Sistema iniciado. Banco de dados pronto.");
        } catch (Exception e) {
            System.out.println("Falha ao iniciar o banco de dados: " + e.getMessage());
            return;
        }

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
