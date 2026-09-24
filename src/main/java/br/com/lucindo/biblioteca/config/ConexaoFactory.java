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
