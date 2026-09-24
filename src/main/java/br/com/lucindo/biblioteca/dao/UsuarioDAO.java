package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Usuario;

import java.sql.Connection;
import java.util.List;

public interface UsuarioDAO {
    void salvar(Connection conexao, Usuario usuario);
    Usuario buscarPorId(Connection conexao, int id);
    List<Usuario> listarTodos(Connection conexao);
}
