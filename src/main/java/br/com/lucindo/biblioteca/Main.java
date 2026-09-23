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
