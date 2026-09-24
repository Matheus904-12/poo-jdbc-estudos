package br.com.lucindo.biblioteca.exception;

public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
