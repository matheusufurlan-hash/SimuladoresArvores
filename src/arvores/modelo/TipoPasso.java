package arvores.modelo;

/**
 * Classifica o passo para o simulador escolher a cor da mensagem.
 *
 * @author Matheus Ubinha Furlan
 */
public enum TipoPasso {
    INFO,       // comparação, descida na árvore
    SUCESSO,    // inserção/remoção feita
    AVISO,      // chave repetida etc.
    ERRO,       // chave não encontrada
    ROTACAO,    // rotações
    AJUSTE,     // troca de cores, alturas
    CONCLUIDO   // último passo da operação
}
