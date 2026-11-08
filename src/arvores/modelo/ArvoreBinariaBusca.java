package arvores.modelo;

/**
 * Árvore Binária de Busca, com o mesmo put/delete recursivo da aula
 * (aesd.ds.implementations.nonlinear.symtable.BinarySearchTree).
 *
 * Remoção de nó com dois filhos, duas opções:
 * - FUSAO (igual à biblioteca da aula): a subárvore direita sobe para o
 *   lugar do nó e a subárvore esquerda é pendurada no menor nó da direita.
 * - HIBBARD (livro do Sedgewick): o sucessor (menor da direita) sai do
 *   lugar dele e ocupa o lugar do nó removido.
 *
 * @author Matheus Ubinha Furlan
 */
public class ArvoreBinariaBusca extends ArvoreAnimada {

    public enum ModoRemocao {
        FUSAO,
        HIBBARD
    }

    private ModoRemocao modoRemocao;

    public ArvoreBinariaBusca() {
        modoRemocao = ModoRemocao.FUSAO;
    }

    @Override
    public String getNome() {
        return "Árvore Binária de Busca";
    }

    public void setModoRemocao( ModoRemocao modoRemocao ) {
        this.modoRemocao = modoRemocao;
    }

    public ModoRemocao getModoRemocao() {
        return modoRemocao;
    }

    // ------------------------------------------------------------------
    // put
    // ------------------------------------------------------------------

    @Override
    protected void put( int chave ) {
        raiz = put( raiz, chave );
        anunciarInsercao( null );
    }

    private No put( No no, int chave ) {

        if ( no == null ) {
            No novo = novoNo( chave );
            recemCriado = novo;
            return novo;
        }

        comparacoes++;
        if ( chave < no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " < " + no.chave + ": desce para a esquerda.",
                   m( no, Destaque.VISITANDO ) );
            no.esquerda = put( no.esquerda, chave );
            anunciarInsercao( no );
        } else if ( chave > no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " > " + no.chave + ": desce para a direita.",
                   m( no, Destaque.VISITANDO ) );
            no.direita = put( no.direita, chave );
            anunciarInsercao( no );
        } else {
            passo( TipoPasso.AVISO, chave + " = " + no.chave
                   + ": a chave já existe. O put só atualiza o valor; a árvore não muda.",
                   m( no, Destaque.ENCONTRADO ) );
        }

        return no;

    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Override
    protected void delete( int chave ) {
        raiz = delete( raiz, chave );
    }

    private No delete( No no, int chave ) {

        if ( no == null ) {
            passo( TipoPasso.ERRO, "Chegou em um link nulo: " + chave + " não está na árvore." );
            return null;
        }

        comparacoes++;
        if ( chave < no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " < " + no.chave + ": desce para a esquerda.",
                   m( no, Destaque.VISITANDO ) );
            no.esquerda = delete( no.esquerda, chave );
            return no;
        }

        if ( chave > no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " > " + no.chave + ": desce para a direita.",
                   m( no, Destaque.VISITANDO ) );
            no.direita = delete( no.direita, chave );
            return no;
        }

        // achou: chave == no.chave

        // caso 1: folha
        if ( no.esquerda == null && no.direita == null ) {
            passo( TipoPasso.INFO, "Achou " + chave + ". É uma folha (sem filhos): basta tirá-la da árvore.",
                   m( no, Destaque.REMOVENDO ) );
            religar( no, null );
            passo( TipoPasso.SUCESSO, chave + " foi removido; o link do pai agora é nulo." );
            return null;
        }

        // caso 2: só um filho
        if ( no.esquerda == null || no.direita == null ) {
            No filho = no.esquerda != null ? no.esquerda : no.direita;
            String lado = no.esquerda != null ? "esquerdo" : "direito";
            passo( TipoPasso.INFO, "Achou " + chave + ". Só tem o filho " + lado + " (" + filho.chave
                   + "): esse filho sobe para o lugar dele.",
                   m( no, Destaque.REMOVENDO ), m( filho, Destaque.SUCESSOR ) );
            religar( no, filho );
            no.esquerda = null;
            no.direita = null;
            passo( TipoPasso.SUCESSO, chave + " foi removido; " + filho.chave + " ocupou o lugar dele.",
                   m( filho, Destaque.SUCESSOR ) );
            return filho;
        }

        // caso 3: dois filhos
        if ( modoRemocao == ModoRemocao.FUSAO ) {
            return removerPorFusao( no );
        }
        return removerPorHibbard( no );

    }

    /**
     * Como na biblioteca da aula: o menor nó da subárvore direita "adota"
     * a subárvore esquerda, e a subárvore direita sobe no lugar do nó.
     */
    private No removerPorFusao( No no ) {

        int chave = no.chave;
        passo( TipoPasso.INFO, "Achou " + chave + ". Tem dois filhos: remoção por fusão.",
               m( no, Destaque.REMOVENDO ) );

        No menor = menorComPassos( no.direita, m( no, Destaque.REMOVENDO ) );
        No subEsquerda = no.esquerda;

        menor.esquerda = subEsquerda;
        no.esquerda = null;
        passo( TipoPasso.AJUSTE, "A subárvore esquerda de " + chave + " (raiz " + subEsquerda.chave
               + ") é pendurada à esquerda de " + menor.chave
               + ". Continua ordenada: tudo nela é menor que " + menor.chave + ".",
               m( no, Destaque.REMOVENDO ), m( menor, Destaque.SUCESSOR ), m( subEsquerda, Destaque.ROTACAO ) );

        No subDireita = no.direita;
        religar( no, subDireita );
        no.direita = null;
        passo( TipoPasso.SUCESSO, chave + " foi removido: a subárvore direita (raiz " + subDireita.chave
               + ") subiu para o lugar dele.",
               m( subDireita, Destaque.SUCESSOR ) );

        return subDireita;

    }

    /**
     * Remoção de Hibbard (Sedgewick):
     *   t = x; x = min(t.right); x.right = deleteMin(t.right); x.left = t.left;
     */
    private No removerPorHibbard( No t ) {

        int chave = t.chave;
        passo( TipoPasso.INFO, "Achou " + chave + ". Tem dois filhos: remoção de Hibbard. "
               + "O sucessor (menor da subárvore direita) vai ocupar o lugar dele.",
               m( t, Destaque.REMOVENDO ) );

        No x = menorComPassos( t.direita, m( t, Destaque.REMOVENDO ) );

        // x.right = deleteMin(t.right)
        if ( x != t.direita ) {
            No paiX = t.direita;
            while ( paiX.esquerda != x ) {
                paiX = paiX.esquerda;
            }
            paiX.esquerda = x.direita;   // o pai de x adota o filho direito de x
            x.direita = t.direita;
        }

        // x.left = t.left
        x.esquerda = t.esquerda;

        religar( t, x );
        t.esquerda = null;
        t.direita = null;

        passo( TipoPasso.SUCESSO, chave + " foi removido: o sucessor " + x.chave
               + " saiu do lugar dele e ocupou a posição de " + chave + ".",
               m( x, Destaque.SUCESSOR ) );

        return x;

    }

}
