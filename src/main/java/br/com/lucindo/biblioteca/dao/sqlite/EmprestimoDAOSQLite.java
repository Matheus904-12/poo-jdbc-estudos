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
