package arvores.modelo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * "Foto" imutável de um nó, tirada em um passo da simulação.
 *
 * A árvore de verdade continua mudando depois da foto; a foto não.
 * É a mesma ideia do vetor copiado a cada passo no simulador de ordenação.
 *
 * A altura e o tamanho são calculados na hora da foto (altura real),
 * por isso o simulador sempre mostra valores corretos.
 *
 * @author Matheus Ubinha Furlan
 */
public final class NoFoto {

    public final int id;
    public final int chave;
    public final boolean vermelho;
    public final NoFoto esquerda;
    public final NoFoto direita;
    public final int altura;   // folha = 1
    public final int tamanho;  // quantidade de nós da subárvore

    public NoFoto( int id, int chave, boolean vermelho, NoFoto esquerda, NoFoto direita ) {
        this.id = id;
        this.chave = chave;
        this.vermelho = vermelho;
        this.esquerda = esquerda;
        this.direita = direita;
        this.altura = Math.max( altura( esquerda ), altura( direita ) ) + 1;
        this.tamanho = tamanho( esquerda ) + tamanho( direita ) + 1;
    }

    /**
     * Fator de balanceamento: altura da esquerda menos altura da direita.
     */
    public int getFatorBalanceamento() {
        return altura( esquerda ) - altura( direita );
    }

    public static int altura( NoFoto no ) {
        return no == null ? 0 : no.altura;
    }

    public static int tamanho( NoFoto no ) {
        return no == null ? 0 : no.tamanho;
    }

    /**
     * Altura negra: quantidade de links pretos de um nó até um link nulo.
     * Em uma Vermelho-Preto correta é igual para todos os caminhos.
     */
    public static int alturaNegra( NoFoto no ) {
        int h = 0;
        while ( no != null ) {
            if ( !no.vermelho ) {
                h++;
            }
            no = no.esquerda;
        }
        return h;
    }

    /**
     * Copia a árvore real para uma árvore de fotos.
     */
    public static NoFoto copiar( No no ) {
        if ( no == null ) {
            return null;
        }
        NoFoto esq = copiar( no.esquerda );
        NoFoto dir = copiar( no.direita );
        return new NoFoto( no.id, no.chave, no.vermelho, esq, dir );
    }

    public static List<Integer> percorrer( NoFoto raiz, Percurso tipo ) {
        List<Integer> chaves = new ArrayList<>();
        switch ( tipo ) {
            case PRE_ORDEM:
                preOrdem( raiz, chaves );
                break;
            case EM_ORDEM:
                emOrdem( raiz, chaves );
                break;
            case POS_ORDEM:
                posOrdem( raiz, chaves );
                break;
            case EM_NIVEL:
                emNivel( raiz, chaves );
                break;
        }
        return chaves;
    }

    private static void preOrdem( NoFoto no, List<Integer> chaves ) {
        if ( no != null ) {
            chaves.add( no.chave );
            preOrdem( no.esquerda, chaves );
            preOrdem( no.direita, chaves );
        }
    }

    private static void emOrdem( NoFoto no, List<Integer> chaves ) {
        if ( no != null ) {
            emOrdem( no.esquerda, chaves );
            chaves.add( no.chave );
            emOrdem( no.direita, chaves );
        }
    }

    private static void posOrdem( NoFoto no, List<Integer> chaves ) {
        if ( no != null ) {
            posOrdem( no.esquerda, chaves );
            posOrdem( no.direita, chaves );
            chaves.add( no.chave );
        }
    }

    private static void emNivel( NoFoto raiz, List<Integer> chaves ) {
        if ( raiz == null ) {
            return;
        }
        Deque<NoFoto> fila = new ArrayDeque<>();
        fila.add( raiz );
        while ( !fila.isEmpty() ) {
            NoFoto no = fila.poll();
            chaves.add( no.chave );
            if ( no.esquerda != null ) {
                fila.add( no.esquerda );
            }
            if ( no.direita != null ) {
                fila.add( no.direita );
            }
        }
    }

}
