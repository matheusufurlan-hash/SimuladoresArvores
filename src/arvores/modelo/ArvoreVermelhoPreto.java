package arvores.modelo;

/**
 * Árvore Vermelho-Preto caída à esquerda (Left-Leaning Red-Black, Sedgewick),
 * com o mesmo algoritmo da aula
 * (aesd.ds.implementations.nonlinear.symtable.RedBlackTree).
 *
 * A cor fica no nó, mas representa o link que chega nele:
 * nó vermelho = link vermelho vindo do pai = os dois nós formam um nó-3
 * da árvore 2-3 equivalente.
 *
 * Regras:
 * - links vermelhos só à esquerda;
 * - nenhum nó tem dois links vermelhos ligados a ele;
 * - todo caminho da raiz até um link nulo tem a mesma quantidade de
 *   links pretos (balanceamento negro perfeito).
 *
 * @author Matheus Ubinha Furlan
 */
public class ArvoreVermelhoPreto extends ArvoreAnimada {

    @Override
    public String getNome() {
        return "Árvore Vermelho-Preto";
    }

    @Override
    protected boolean usaRotacoes() {
        return true;
    }

    @Override
    protected String complementoInsercao( No novo ) {
        return " Todo nó novo entra vermelho (ligado ao pai por um link vermelho).";
    }

    private boolean isRed( No no ) {
        return no != null && no.vermelho;
    }

    // ------------------------------------------------------------------
    // put
    // ------------------------------------------------------------------

    @Override
    protected void put( int chave ) {
        raiz = put( raiz, chave );
        anunciarInsercao( null );
        if ( raiz.vermelho ) {
            raiz.vermelho = false;
            passo( TipoPasso.AJUSTE, "A raiz é sempre preta: " + raiz.chave + " é pintada de preto.",
                   m( raiz, Destaque.CORES ) );
        }
    }

    private No put( No h, int chave ) {

        if ( h == null ) {
            No novo = novoNo( chave );
            novo.vermelho = true;
            recemCriado = novo;
            return novo;
        }

        comparacoes++;
        if ( chave < h.chave ) {
            visitar( h );
            passo( TipoPasso.INFO, chave + " < " + h.chave + ": desce para a esquerda.",
                   m( h, Destaque.VISITANDO ) );
            h.esquerda = put( h.esquerda, chave );
            anunciarInsercao( h );
        } else if ( chave > h.chave ) {
            visitar( h );
            passo( TipoPasso.INFO, chave + " > " + h.chave + ": desce para a direita.",
                   m( h, Destaque.VISITANDO ) );
            h.direita = put( h.direita, chave );
            anunciarInsercao( h );
        } else {
            passo( TipoPasso.AVISO, chave + " = " + h.chave
                   + ": a chave já existe. O put só atualiza o valor; a árvore não muda.",
                   m( h, Destaque.ENCONTRADO ) );
        }

        // consertando os links inclinados à direita (na volta da recursão)
        if ( isRed( h.direita ) && !isRed( h.esquerda ) ) {
            passo( TipoPasso.ROTACAO, "Em " + h.chave + ": link vermelho inclinado para a direita (filho "
                   + h.direita.chave + "). Rotação à esquerda.",
                   m( h, Destaque.ROTACAO ), m( h.direita, Destaque.ROTACAO ) );
            h = rotacaoEsquerda( h );
        }
        if ( isRed( h.esquerda ) && isRed( h.esquerda.esquerda ) ) {
            passo( TipoPasso.ROTACAO, "Em " + h.chave + ": dois links vermelhos seguidos à esquerda ("
                   + h.esquerda.chave + " e " + h.esquerda.esquerda.chave + "). Rotação à direita.",
                   m( h, Destaque.ROTACAO ), m( h.esquerda, Destaque.ROTACAO ),
                   m( h.esquerda.esquerda, Destaque.ROTACAO ) );
            h = rotacaoDireita( h );
        }
        if ( isRed( h.esquerda ) && isRed( h.direita ) ) {
            passo( TipoPasso.AJUSTE, "Em " + h.chave + ": os dois filhos são vermelhos (um nó-4 temporário). "
                   + "Inversão de cores (flipColors).",
                   m( h, Destaque.CORES ), m( h.esquerda, Destaque.CORES ), m( h.direita, Destaque.CORES ) );
            inverterCores( h );
        }

        return h;

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

        // se ambos os filhos da raiz forem pretos, a raiz fica vermelha
        if ( !isRed( raiz.esquerda ) && !isRed( raiz.direita ) ) {
            raiz.vermelho = true;
            passo( TipoPasso.AJUSTE, "Os dois filhos da raiz são pretos: a raiz " + raiz.chave
                   + " é pintada de vermelho (temporário) para a descida não parar em um nó-2.",
                   m( raiz, Destaque.CORES ) );
        }

        raiz = delete( raiz, chave );

        if ( raiz != null && raiz.vermelho ) {
            raiz.vermelho = false;
            passo( TipoPasso.AJUSTE, "A raiz volta a ser preta.", m( raiz, Destaque.CORES ) );
        }

    }

    private No delete( No h, int chave ) {

        comparacoes++;
        if ( chave < h.chave ) {

            visitar( h );
            passo( TipoPasso.INFO, chave + " < " + h.chave + ": vai descer para a esquerda.",
                   m( h, Destaque.VISITANDO ) );

            // antes de descer, garante que o filho esquerdo não seja um nó-2
            if ( !isRed( h.esquerda ) && !isRed( h.esquerda.esquerda ) ) {
                passo( TipoPasso.AJUSTE, "O filho esquerdo " + h.esquerda.chave
                       + " é um nó-2 (preto e sem filho esquerdo vermelho): moveRedLeft em " + h.chave + ".",
                       m( h, Destaque.CORES ), m( h.esquerda, Destaque.CORES ) );
                h = moveRedLeft( h );
            }
            h.esquerda = delete( h.esquerda, chave );

        } else {

            visitar( h );
            if ( chave > h.chave ) {
                passo( TipoPasso.INFO, chave + " > " + h.chave + ": vai descer para a direita.",
                       m( h, Destaque.VISITANDO ) );
            } else if ( h.direita != null || isRed( h.esquerda ) ) {
                passo( TipoPasso.INFO, chave + " = " + h.chave + ": chegou na chave procurada. "
                       + "Antes de remover, ajusta os links em volta dela.", m( h, Destaque.VISITANDO ) );
            }
            boolean ajustou = false;

            // inclina o link vermelho para a direita
            if ( isRed( h.esquerda ) ) {
                passo( TipoPasso.ROTACAO, "Em " + h.chave + " (" + chave + " >= " + h.chave
                       + "): o link vermelho da esquerda é girado para a direita. Rotação à direita.",
                       m( h, Destaque.ROTACAO ), m( h.esquerda, Destaque.ROTACAO ) );
                h = rotacaoDireita( h );
                ajustou = true;
            }

            // achou e não tem filho à direita: é uma folha
            if ( chave == h.chave && h.direita == null ) {
                passo( TipoPasso.INFO, "Achou " + chave + " e ele não tem filho à direita (é uma folha): sai da árvore.",
                       m( h, Destaque.REMOVENDO ) );
                religar( h, null );
                passo( TipoPasso.SUCESSO, "Nó " + chave + " removido." );
                return null;
            }

            // garante que não se desça a um nó-2 pela direita
            if ( !isRed( h.direita ) && !isRed( h.direita.esquerda ) ) {
                passo( TipoPasso.AJUSTE, "O filho direito " + h.direita.chave
                       + " é um nó-2 (preto e sem filho esquerdo vermelho): moveRedRight em " + h.chave + ".",
                       m( h, Destaque.CORES ), m( h.direita, Destaque.CORES ) );
                h = moveRedRight( h );
                ajustou = true;
            }

            if ( chave == h.chave ) {
                // dois filhos: copia o sucessor e remove o sucessor com deleteMin
                passo( TipoPasso.INFO, "Achou " + chave + ". Vai receber a chave do sucessor e o sucessor "
                       + "sai da subárvore direita com deleteMin.", m( h, Destaque.REMOVENDO ) );
                No x = menorComPassos( h.direita, m( h, Destaque.REMOVENDO ) );
                h.chave = x.chave;
                passoCopia( x, h, "A chave " + x.chave + " é copiada para o lugar de " + chave
                            + ". Agora deleteMin remove o " + x.chave + " original da subárvore direita.",
                            m( h, Destaque.SUCESSOR ), m( x, Destaque.REMOVENDO ) );
                h.direita = deleteMin( h.direita );
            } else {
                if ( ajustou ) {
                    // depois dos ajustes o nó atual pode ter mudado: mostra de novo a decisão
                    visitar( h );
                    passo( TipoPasso.INFO, chave + " > " + h.chave + ": desce para a direita.",
                           m( h, Destaque.VISITANDO ) );
                }
                h.direita = delete( h.direita, chave );
            }

        }

        // desfaz as violações deixadas pela descida
        return balance( h );

    }

    private No deleteMin( No h ) {

        if ( h.esquerda == null ) {
            passo( TipoPasso.INFO, "deleteMin: " + h.chave + " não tem filho esquerdo, é o menor. Sai da árvore.",
                   m( h, Destaque.REMOVENDO ) );
            religar( h, null );
            passo( TipoPasso.SUCESSO, "O sucessor original (" + h.chave + ") foi removido." );
            return null;
        }

        visitar( h );
        passo( TipoPasso.INFO, "deleteMin: desce para a esquerda de " + h.chave + ".",
               m( h, Destaque.VISITANDO ) );

        if ( !isRed( h.esquerda ) && !isRed( h.esquerda.esquerda ) ) {
            passo( TipoPasso.AJUSTE, "O filho esquerdo " + h.esquerda.chave
                   + " é um nó-2: moveRedLeft em " + h.chave + ".",
                   m( h, Destaque.CORES ), m( h.esquerda, Destaque.CORES ) );
            h = moveRedLeft( h );
        }

        h.esquerda = deleteMin( h.esquerda );
        return balance( h );

    }

    // ------------------------------------------------------------------
    // rotações, cores e ajustes
    // ------------------------------------------------------------------

    private No rotacaoDireita( No h ) {
        rotacoes++;
        No x = h.esquerda;
        h.esquerda = x.direita;
        x.direita = h;
        x.vermelho = h.vermelho;
        h.vermelho = true;
        religar( h, x );
        passo( TipoPasso.ROTACAO, "Rotação à direita em " + h.chave + ": " + x.chave + " sobe (herda a cor de "
               + h.chave + ") e " + h.chave + " desce para a direita, vermelho.",
               m( x, Destaque.ROTACAO ), m( h, Destaque.ROTACAO ) );
        return x;
    }

    private No rotacaoEsquerda( No h ) {
        rotacoes++;
        No x = h.direita;
        h.direita = x.esquerda;
        x.esquerda = h;
        x.vermelho = h.vermelho;
        h.vermelho = true;
        religar( h, x );
        passo( TipoPasso.ROTACAO, "Rotação à esquerda em " + h.chave + ": " + x.chave + " sobe (herda a cor de "
               + h.chave + ") e " + h.chave + " desce para a esquerda, vermelho.",
               m( x, Destaque.ROTACAO ), m( h, Destaque.ROTACAO ) );
        return x;
    }

    private void inverterCores( No h ) {
        h.vermelho = !h.vermelho;
        h.esquerda.vermelho = !h.esquerda.vermelho;
        h.direita.vermelho = !h.direita.vermelho;
        passo( TipoPasso.AJUSTE, "flipColors em " + h.chave + ": " + h.chave + " e seus filhos "
               + h.esquerda.chave + " e " + h.direita.chave + " trocam de cor.",
               m( h, Destaque.CORES ), m( h.esquerda, Destaque.CORES ), m( h.direita, Destaque.CORES ) );
    }

    private No moveRedLeft( No h ) {
        inverterCores( h );
        if ( isRed( h.direita.esquerda ) ) {
            passo( TipoPasso.ROTACAO, "O irmão " + h.direita.chave + " tem filho esquerdo vermelho ("
                   + h.direita.esquerda.chave + "): pega uma chave emprestada com duas rotações.",
                   m( h, Destaque.ROTACAO ), m( h.direita, Destaque.ROTACAO ),
                   m( h.direita.esquerda, Destaque.ROTACAO ) );
            h.direita = rotacaoDireita( h.direita );
            h = rotacaoEsquerda( h );
            inverterCores( h );
        }
        return h;
    }

    private No moveRedRight( No h ) {
        inverterCores( h );
        if ( isRed( h.esquerda.esquerda ) ) {
            passo( TipoPasso.ROTACAO, "O irmão " + h.esquerda.chave + " tem filho esquerdo vermelho ("
                   + h.esquerda.esquerda.chave + "): pega uma chave emprestada com uma rotação à direita.",
                   m( h, Destaque.ROTACAO ), m( h.esquerda, Destaque.ROTACAO ) );
            h = rotacaoDireita( h );
            inverterCores( h );
        }
        return h;
    }

    private No balance( No h ) {
        if ( isRed( h.direita ) ) {
            passo( TipoPasso.ROTACAO, "Subindo, em " + h.chave + ": link vermelho à direita ("
                   + h.direita.chave + "). Rotação à esquerda.",
                   m( h, Destaque.ROTACAO ), m( h.direita, Destaque.ROTACAO ) );
            h = rotacaoEsquerda( h );
        }
        if ( isRed( h.esquerda ) && isRed( h.esquerda.esquerda ) ) {
            passo( TipoPasso.ROTACAO, "Subindo, em " + h.chave + ": dois links vermelhos seguidos à esquerda. "
                   + "Rotação à direita.",
                   m( h, Destaque.ROTACAO ), m( h.esquerda, Destaque.ROTACAO ) );
            h = rotacaoDireita( h );
        }
        if ( isRed( h.esquerda ) && isRed( h.direita ) ) {
            passo( TipoPasso.AJUSTE, "Subindo, em " + h.chave + ": os dois filhos são vermelhos. Inversão de cores.",
                   m( h, Destaque.CORES ), m( h.esquerda, Destaque.CORES ), m( h.direita, Destaque.CORES ) );
            inverterCores( h );
        }
        return h;
    }

}
