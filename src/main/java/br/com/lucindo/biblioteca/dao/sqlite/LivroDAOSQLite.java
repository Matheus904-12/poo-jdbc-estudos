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
