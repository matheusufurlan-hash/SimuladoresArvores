package arvores.modelo;

/**
 * Nó usado pelas três árvores.
 *
 * Cada árvore usa só os campos de que precisa:
 * - a AVL usa "altura";
 * - a Vermelho-Preto usa "vermelho" (cor do link que chega no nó).
 *
 * O "id" nunca muda. É ele que permite ao simulador saber que um nó
 * mudou de lugar (rotação, remoção etc.) e animar o movimento.
 *
 * @author Matheus Ubinha Furlan
 */
public class No {

    public final int id;
    public int chave;
    public No esquerda;
    public No direita;

    // usado pela AVL: folha tem altura 1, link nulo tem altura 0
    public int altura;

    // usado pela Vermelho-Preto
    public boolean vermelho;

    public No( int id, int chave ) {
        this.id = id;
        this.chave = chave;
        this.altura = 1;
    }

}
