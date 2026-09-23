package br.com.lucindo.biblioteca.model;

import java.time.LocalDate;

public class Emprestimo {
    public enum Status { ATIVO, DEVOLVIDO }

    private Integer id;
    private int livroId;
    private int usuarioId;
    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataRealDevolucao;
    private Status status;

    public Emprestimo(int livroId, int usuarioId, LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao) {
        this.livroId = livroId;
        this.usuarioId = usuarioId;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
        this.status = Status.ATIVO;
    }

    public Emprestimo(Integer id, int livroId, int usuarioId, LocalDate dataEmprestimo,
                       LocalDate dataPrevistaDevolucao, LocalDate dataRealDevolucao, Status status) {
        this.id = id;
        this.livroId = livroId;
        this.usuarioId = usuarioId;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
        this.dataRealDevolucao = dataRealDevolucao;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getLivroId() { return livroId; }
    public int getUsuarioId() { return usuarioId; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public LocalDate getDataRealDevolucao() { return dataRealDevolucao; }
    public void setDataRealDevolucao(LocalDate dataRealDevolucao) { this.dataRealDevolucao = dataRealDevolucao; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("#%d | Livro #%d -> Usuario #%d | Emprestado em %s | Previsto: %s | Status: %s",
                id, livroId, usuarioId, dataEmprestimo, dataPrevistaDevolucao, status);
    }
}
