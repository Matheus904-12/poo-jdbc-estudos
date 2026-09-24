CREATE TABLE IF NOT EXISTS livro (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL,
    autor TEXT NOT NULL,
    isbn TEXT UNIQUE NOT NULL,
    quantidade_total INTEGER NOT NULL,
    quantidade_disponivel INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS emprestimo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    livro_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,
    data_emprestimo TEXT NOT NULL,
    data_prevista_devolucao TEXT NOT NULL,
    data_real_devolucao TEXT,
    status TEXT NOT NULL,
    FOREIGN KEY (livro_id) REFERENCES livro(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
