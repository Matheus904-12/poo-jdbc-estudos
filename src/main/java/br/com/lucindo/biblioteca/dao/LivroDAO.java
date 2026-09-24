package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Livro;

import java.sql.Connection;
import java.util.List;

public interface LivroDAO {
    void salvar(Connection conexao, Livro livro);
    Livro buscarPorId(Connection conexao, int id);
    List<Livro> listarDisponiveis(Connection conexao);
    void atualizar(Connection conexao, Livro livro);
}
