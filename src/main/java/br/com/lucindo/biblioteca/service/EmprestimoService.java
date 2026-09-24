package br.com.lucindo.biblioteca.service;

import br.com.lucindo.biblioteca.config.ConexaoFactory;
import br.com.lucindo.biblioteca.dao.EmprestimoDAO;
import br.com.lucindo.biblioteca.dao.LivroDAO;
import br.com.lucindo.biblioteca.dao.UsuarioDAO;
import br.com.lucindo.biblioteca.exception.RepositorioException;
import br.com.lucindo.biblioteca.model.Emprestimo;
import br.com.lucindo.biblioteca.model.Livro;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EmprestimoService {
    private static final int PRAZO_DIAS = 14;
    private static final BigDecimal MULTA_POR_DIA = new BigDecimal("2.00");

    private final LivroDAO livroDAO;
    private final UsuarioDAO usuarioDAO;
    private final EmprestimoDAO emprestimoDAO;

    public EmprestimoService(LivroDAO livroDAO, UsuarioDAO usuarioDAO, EmprestimoDAO emprestimoDAO) {
        this.livroDAO = livroDAO;
        this.usuarioDAO = usuarioDAO;
        this.emprestimoDAO = emprestimoDAO;
    }

    public Emprestimo registrarEmprestimo(int livroId, int usuarioId) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            Livro livro = livroDAO.buscarPorId(conexao, livroId);
            if (livro == null) {
                throw new IllegalStateException("Livro nao encontrado: " + livroId);
            }
            if (usuarioDAO.buscarPorId(conexao, usuarioId) == null) {
                throw new IllegalStateException("Usuario nao encontrado: " + usuarioId);
            }
            if (livro.getQuantidadeDisponivel() <= 0) {
                throw new IllegalStateException("Nao ha exemplares disponiveis para o livro: " + livro.getTitulo());
            }

            LocalDate hoje = LocalDate.now();
            Emprestimo emprestimo = new Emprestimo(livroId, usuarioId, hoje, hoje.plusDays(PRAZO_DIAS));
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);

            conexao.setAutoCommit(false);
            try {
                emprestimoDAO.salvar(conexao, emprestimo);
                livroDAO.atualizar(conexao, livro);
                conexao.commit();
            } catch (RuntimeException e) {
                conexao.rollback();
                throw e;
            }

            return emprestimo;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar emprestimo", e);
        }
    }

    public BigDecimal registrarDevolucao(int emprestimoId) {
        try (Connection conexao = ConexaoFactory.obterConexao()) {
            Emprestimo emprestimo = emprestimoDAO.buscarPorId(conexao, emprestimoId);
            if (emprestimo == null) {
                throw new IllegalStateException("Emprestimo nao encontrado: " + emprestimoId);
            }
            if (emprestimo.getStatus() == Emprestimo.Status.DEVOLVIDO) {
                throw new IllegalStateException("Emprestimo ja foi devolvido: " + emprestimoId);
            }

            Livro livro = livroDAO.buscarPorId(conexao, emprestimo.getLivroId());
            LocalDate hoje = LocalDate.now();
            BigDecimal multa = calcularMulta(emprestimo.getDataPrevistaDevolucao(), hoje);

            emprestimo.setDataRealDevolucao(hoje);
            emprestimo.setStatus(Emprestimo.Status.DEVOLVIDO);
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() + 1);

            conexao.setAutoCommit(false);
            try {
                emprestimoDAO.atualizar(conexao, emprestimo);
                livroDAO.atualizar(conexao, livro);
                conexao.commit();
            } catch (RuntimeException e) {
                conexao.rollback();
                throw e;
            }

            return multa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar devolucao", e);
        }
    }

    private BigDecimal calcularMulta(LocalDate dataPrevista, LocalDate dataReal) {
        long diasAtraso = ChronoUnit.DAYS.between(dataPrevista, dataReal);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO;
        }
        return MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso));
    }
}
