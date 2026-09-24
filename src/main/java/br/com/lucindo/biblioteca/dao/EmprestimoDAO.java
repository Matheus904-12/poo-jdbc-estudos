package br.com.lucindo.biblioteca.dao;

import br.com.lucindo.biblioteca.model.Emprestimo;

import java.sql.Connection;
import java.util.List;

public interface EmprestimoDAO {
    void salvar(Connection conexao, Emprestimo emprestimo);
    Emprestimo buscarPorId(Connection conexao, int id);
    List<Emprestimo> listarAtivos(Connection conexao);
    void atualizar(Connection conexao, Emprestimo emprestimo);
}
