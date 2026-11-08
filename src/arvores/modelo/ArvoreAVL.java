package arvores.modelo;

/**
 * Árvore AVL, com o mesmo algoritmo da aula
 * (aesd.ds.implementations.nonlinear.symtable.AVLTree):
 * depois de cada put/delete recursivo, "balance" confere o fator de
 * balanceamento de cada nó do caminho e faz rotações quando |FB| > 1.
 *
 * Altura: folha = 1, link nulo = 0.
 * FB = altura(esquerda) - altura(direita).
 *
 * @author Matheus Ubinha Furlan
 */
public class ArvoreAVL extends ArvoreAnimada {

    private static final int DESBALANCEAMENTO_PERMITIDO = 1;

    // mostrar um passo para cada nó conferido na volta da recursão
    private boolean mostrarVerificacoes;

    public ArvoreAVL() {
        mostrarVerificacoes = true;
    }

    @Override
    public String getNome() {
        return "Árvore AVL";
    }

    @Override
    protected boolean usaRotacoes() {
        return true;
    }

    public void setMostrarVerificacoes( boolean mostrarVerificacoes ) {
        this.mostrarVerificacoes = mostrarVerificacoes;
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
            novo.altura = 1;
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
            return no;
        }

        return balance( no );

    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Override
    protected void delete( int chave ) {
        // como na aula: se a chave não existe, não faz nada
        if ( !contem( chave ) ) {
            get( chave );
            return;
        }
        raiz = delete( raiz, chave );
    }

    private No delete( No no, int chave ) {

        if ( no == null ) {
            return null;
        }

        comparacoes++;
        if ( chave < no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " < " + no.chave + ": desce para a esquerda.",
                   m( no, Destaque.VISITANDO ) );
            no.esquerda = delete( no.esquerda, chave );
        } else if ( chave > no.chave ) {
            visitar( no );
            passo( TipoPasso.INFO, chave + " > " + no.chave + ": desce para a direita.",
                   m( no, Destaque.VISITANDO ) );
            no.direita = delete( no.direita, chave );
        } else if ( no.esquerda != null && no.direita != null ) {

            // dois filhos: copia a chave do sucessor e remove o sucessor
            passo( TipoPasso.INFO, "Achou " + chave + ". Tem dois filhos: vai receber a chave do sucessor "
                   + "(o menor da subárvore direita).", m( no, Destaque.REMOVENDO ) );
            No menor = menorComPassos( no.direita, m( no, Destaque.REMOVENDO ) );
            no.chave = menor.chave;
            passoCopia( menor, no, "A chave " + menor.chave + " é copiada para o lugar de " + chave
                        + ". Agora " + menor.chave + " aparece duas vezes: o original será removido da subárvore direita.",
                        m( no, Destaque.SUCESSOR ), m( menor, Destaque.REMOVENDO ) );
            no.direita = delete( no.direita, no.chave );

        } else {

            // zero ou um filho
            No filho = ( no.esquerda != null ) ? no.esquerda : no.direita;
            if ( filho == null ) {
                passo( TipoPasso.INFO, "Achou " + chave + ". É uma folha: basta tirá-la da árvore.",
                       m( no, Destaque.REMOVENDO ) );
            } else {
                passo( TipoPasso.INFO, "Achou " + chave + ". Só tem um filho (" + filho.chave
                       + "): ele sobe para o lugar de " + chave + ".",
                       m( no, Destaque.REMOVENDO ), m( filho, Destaque.SUCESSOR ) );
            }
            religar( no, filho );
            passo( TipoPasso.SUCESSO, "Nó " + chave + " removido.", m( filho, Destaque.SUCESSOR ) );
            no = filho;

        }

        return balance( no );

    }

    // ------------------------------------------------------------------
    // balanceamento
    // ------------------------------------------------------------------

    private int altura( No no ) {
        return no == null ? 0 : no.altura;
    }

    private void atualizarAltura( No no ) {
        no.altura = Math.max( altura( no.esquerda ), altura( no.direita ) ) + 1;
    }

    private No balance( No no ) {

        if ( no == null ) {
            return null;
        }

        int fb = altura( no.esquerda ) - altura( no.direita );

        if ( fb > DESBALANCEAMENTO_PERMITIDO ) {

            No filho = no.esquerda;
            if ( altura( filho.esquerda ) >= altura( filho.direita ) ) {
                passo( TipoPasso.ROTACAO, "Nó " + no.chave + " desbalanceado (FB = +" + fb
                       + "): pende para a esquerda e o filho " + filho.chave
                       + " também (caso Esquerda-Esquerda). Rotação simples à direita.",
                       m( no, Destaque.DESBALANCEADO ), m( filho, Destaque.ROTACAO ) );
                no = rotacaoDireita( no );
            } else {
                passo( TipoPasso.ROTACAO, "Nó " + no.chave + " desbalanceado (FB = +" + fb
                       + ") e o filho " + filho.chave + " pende para a direita (caso Esquerda-Direita). "
                       + "Rotação dupla: esquerda em " + filho.chave + ", depois direita em " + no.chave + ".",
                       m( no, Destaque.DESBALANCEADO ), m( filho, Destaque.ROTACAO ),
                       m( filho.direita, Destaque.ROTACAO ) );
                no.esquerda = rotacaoEsquerda( no.esquerda );
                no = rotacaoDireita( no );
            }

        } else if ( -fb > DESBALANCEAMENTO_PERMITIDO ) {

            No filho = no.direita;
            if ( altura( filho.direita ) >= altura( filho.esquerda ) ) {
                passo( TipoPasso.ROTACAO, "Nó " + no.chave + " desbalanceado (FB = " + fb
                       + "): pende para a direita e o filho " + filho.chave
                       + " também (caso Direita-Direita). Rotação simples à esquerda.",
                       m( no, Destaque.DESBALANCEADO ), m( filho, Destaque.ROTACAO ) );
                no = rotacaoEsquerda( no );
            } else {
                passo( TipoPasso.ROTACAO, "Nó " + no.chave + " desbalanceado (FB = " + fb
                       + ") e o filho " + filho.chave + " pende para a esquerda (caso Direita-Esquerda). "
                       + "Rotação dupla: direita em " + filho.chave + ", depois esquerda em " + no.chave + ".",
                       m( no, Destaque.DESBALANCEADO ), m( filho, Destaque.ROTACAO ),
                       m( filho.esquerda, Destaque.ROTACAO ) );
                no.direita = rotacaoDireita( no.direita );
                no = rotacaoEsquerda( no );
            }

        } else if ( mostrarVerificacoes ) {

            int novaAltura = Math.max( altura( no.esquerda ), altura( no.direita ) ) + 1;
            String sinal = fb > 0 ? "+" : "";
            passo( TipoPasso.AJUSTE, "Volta da recursão em " + no.chave + ": altura " + novaAltura
                   + ", FB = " + sinal + fb + ". Está balanceado (FB entre -1 e +1).",
                   m( no, Destaque.VISITANDO ) );

        }

        atualizarAltura( no );
        return no;

    }

    /**
     * rotateWithLeftChild da aula: o filho esquerdo sobe.
     */
    private No rotacaoDireita( No a ) {
        rotacoes++;
        No b = a.esquerda;
        a.esquerda = b.direita;
        b.direita = a;
        atualizarAltura( a );
        atualizarAltura( b );
        religar( a, b );
        passo( TipoPasso.ROTACAO, "Rotação à direita em " + a.chave + " (rotateWithLeftChild): "
               + b.chave + " sobe e " + a.chave + " vira o filho direito dele.",
               m( b, Destaque.ROTACAO ), m( a, Destaque.ROTACAO ) );
        return b;
    }

    /**
     * rotateWithRightChild da aula: o filho direito sobe.
     */
    private No rotacaoEsquerda( No a ) {
        rotacoes++;
        No b = a.direita;
        a.direita = b.esquerda;
        b.esquerda = a;
        atualizarAltura( a );
        atualizarAltura( b );
        religar( a, b );
        passo( TipoPasso.ROTACAO, "Rotação à esquerda em " + a.chave + " (rotateWithRightChild): "
               + b.chave + " sobe e " + a.chave + " vira o filho esquerdo dele.",
               m( b, Destaque.ROTACAO ), m( a, Destaque.ROTACAO ) );
        return b;
    }

}
