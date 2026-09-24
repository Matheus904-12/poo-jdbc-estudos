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
