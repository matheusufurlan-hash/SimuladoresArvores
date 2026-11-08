package arvores.modelo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base das três árvores do simulador.
 *
 * Cada árvore implementa put e delete do mesmo jeito que na aula
 * (biblioteca aesd do Prof. Buzatto), mas chamando "passo(...)" nos pontos
 * importantes. Cada chamada guarda uma foto da árvore e um texto explicando
 * o que aconteceu. O simulador depois mostra essas fotos em sequência.
 *
 * @author Matheus Ubinha Furlan
 */
public abstract class ArvoreAnimada {

    /**
     * Par (nó, destaque) usado para marcar nós em um passo.
     */
    public record Marca( No no, Destaque destaque ) {}

    /**
     * Cópia completa da árvore, usada para desfazer operações.
     */
    public static final class Estado {
        private final No raiz;
        private final int proximoId;
        private final String descricao;

        private Estado( No raiz, int proximoId, String descricao ) {
            this.raiz = raiz;
            this.proximoId = proximoId;
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    protected No raiz;

    // contadores da operação atual
    protected int comparacoes;
    protected int rotacoes;

    // nó criado na base da recursão do put, esperando para ser anunciado
    protected No recemCriado;

    private int proximoId;
    private boolean gravando;
    private final List<Passo> passos;
    private final Set<Integer> caminho;
    private String operacao;

    public ArvoreAnimada() {
        raiz = null;
        proximoId = 1;
        gravando = true;
        passos = new ArrayList<>();
        caminho = new LinkedHashSet<>();
        operacao = "";
    }

    public abstract String getNome();

    /**
     * Insere a chave. Deve chamar passo(...) nos pontos importantes.
     */
    protected abstract void put( int chave );

    /**
     * Remove a chave. Deve chamar passo(...) nos pontos importantes.
     */
    protected abstract void delete( int chave );

    /**
     * Diz se a árvore faz rotações (AVL e Vermelho-Preto).
     */
    protected boolean usaRotacoes() {
        return false;
    }

    /**
     * Texto extra dito quando um nó novo entra na árvore.
     */
    protected String complementoInsercao( No novo ) {
        return "";
    }

    // ------------------------------------------------------------------
    // operações públicas: cada uma devolve a lista de passos para animar
    // ------------------------------------------------------------------

    public List<Passo> inserir( int chave ) {
        iniciar( "put(" + chave + ")" );
        boolean existia = contem( chave );
        put( chave );
        if ( existia ) {
            concluir( "put(" + chave + ") concluído: a chave já existia, então só o valor foi atualizado.",
                      m( buscarNo( chave ), Destaque.ENCONTRADO ) );
        } else {
            concluir( "put(" + chave + ") concluído.", m( buscarNo( chave ), Destaque.NOVO ) );
        }
        return pegarPassos();
    }

    public List<Passo> remover( int chave ) {
        iniciar( "delete(" + chave + ")" );
        boolean existia = contem( chave );
        delete( chave );
        if ( existia ) {
            concluir( "delete(" + chave + ") concluído." );
        } else {
            concluir( "delete(" + chave + ") concluído: a chave não estava na árvore, nada mudou." );
        }
        return pegarPassos();
    }

    public List<Passo> buscar( int chave ) {
        iniciar( "get(" + chave + ")" );
        boolean achou = get( chave );
        if ( achou ) {
            concluir( "get(" + chave + "): chave encontrada.", m( buscarNo( chave ), Destaque.ENCONTRADO ) );
        } else {
            concluir( "get(" + chave + "): chave não encontrada, retorna null." );
        }
        return pegarPassos();
    }

    /**
     * Busca igual para as três árvores (todas são árvores binárias de busca).
     * Devolve true se achou.
     */
    protected boolean get( int chave ) {
        No atual = raiz;
        if ( atual == null ) {
            passo( TipoPasso.ERRO, "A árvore está vazia: " + chave + " não está nela." );
            return false;
        }
        while ( atual != null ) {
            comparacoes++;
            if ( chave < atual.chave ) {
                visitar( atual );
                passo( TipoPasso.INFO, chave + " < " + atual.chave + ": desce para a esquerda.",
                       m( atual, Destaque.VISITANDO ) );
                atual = atual.esquerda;
            } else if ( chave > atual.chave ) {
                visitar( atual );
                passo( TipoPasso.INFO, chave + " > " + atual.chave + ": desce para a direita.",
                       m( atual, Destaque.VISITANDO ) );
                atual = atual.direita;
            } else {
                passo( TipoPasso.SUCESSO, chave + " = " + atual.chave + ": chave encontrada!",
                       m( atual, Destaque.ENCONTRADO ) );
                return true;
            }
        }
        passo( TipoPasso.ERRO, "Chegou em um link nulo: " + chave + " não está na árvore." );
        return false;
    }

    // ------------------------------------------------------------------
    // ferramentas para as subclasses
    // ------------------------------------------------------------------

    protected No novoNo( int chave ) {
        return new No( proximoId++, chave );
    }

    protected static Marca m( No no, Destaque destaque ) {
        return new Marca( no, destaque );
    }

    /**
     * Guarda um passo: foto da árvore agora + texto + nós destacados.
     */
    protected void passo( TipoPasso tipo, String texto, Marca... marcas ) {
        if ( gravando ) {
            passos.add( criarPasso( tipo, texto, -1, -1, marcas ) );
        }
    }

    /**
     * Passo em que a chave de "de" foi copiada para "para"
     * (o simulador anima a chave voando de um nó para o outro).
     */
    protected void passoCopia( No de, No para, String texto, Marca... marcas ) {
        if ( gravando ) {
            passos.add( criarPasso( TipoPasso.AJUSTE, texto, de.id, para.id, marcas ) );
        }
    }

    /**
     * Marca o nó como parte do caminho da operação atual.
     */
    protected void visitar( No no ) {
        if ( no != null ) {
            caminho.add( no.id );
        }
    }

    /**
     * Mostra um nó novo assim que ele é ligado ao pai.
     * É chamado logo depois de "no.esquerda = put( no.esquerda, ... )".
     */
    protected void anunciarInsercao( No pai ) {
        if ( recemCriado == null ) {
            return;
        }
        No novo = recemCriado;
        recemCriado = null;
        if ( pai == null ) {
            passo( TipoPasso.SUCESSO, "A árvore estava vazia: " + novo.chave + " vira a raiz."
                   + complementoInsercao( novo ), m( novo, Destaque.NOVO ) );
        } else {
            String lado = pai.esquerda == novo ? "esquerdo" : "direito";
            passo( TipoPasso.SUCESSO, "Achou um link nulo: " + novo.chave + " entra como filho "
                   + lado + " de " + pai.chave + "." + complementoInsercao( novo ),
                   m( novo, Destaque.NOVO ) );
        }
    }

    /**
     * Procura o menor nó a partir de "inicio", mostrando a descida.
     * As marcas "fixas" continuam aparecendo em todos os passos.
     */
    protected No menorComPassos( No inicio, Marca... fixas ) {
        No atual = inicio;
        visitar( atual );
        passo( TipoPasso.INFO, "Procura o menor da subárvore direita: começa no filho direito ("
               + atual.chave + ").", juntar( fixas, m( atual, Destaque.VISITANDO ) ) );
        while ( atual.esquerda != null ) {
            atual = atual.esquerda;
            visitar( atual );
            passo( TipoPasso.INFO, "Segue sempre para a esquerda: " + atual.chave + ".",
                   juntar( fixas, m( atual, Destaque.VISITANDO ) ) );
        }
        passo( TipoPasso.INFO, atual.chave + " não tem filho esquerdo: é o menor da subárvore direita (o sucessor).",
               juntar( fixas, m( atual, Destaque.SUCESSOR ) ) );
        return atual;
    }

    protected static Marca[] juntar( Marca[] fixas, Marca... outras ) {
        Marca[] todas = new Marca[fixas.length + outras.length];
        System.arraycopy( fixas, 0, todas, 0, fixas.length );
        System.arraycopy( outras, 0, todas, fixas.length, outras.length );
        return todas;
    }

    /**
     * Troca, no pai de "antigo", o link que apontava para "antigo" por "novo".
     *
     * Por que isso existe: no código recursivo da aula, o pai só recebe a
     * nova subárvore quando a recursão volta ("no.esquerda = put(...)").
     * Se a foto fosse tirada antes disso, o nó que subiu numa rotação
     * apareceria solto. Religando na hora, a foto fica certa; quando a
     * recursão voltar, ela faz a mesma atribuição de novo (não muda nada).
     */
    protected void religar( No antigo, No novo ) {
        if ( antigo == novo ) {
            return;
        }
        if ( raiz == antigo ) {
            raiz = novo;
            return;
        }
        No pai = encontrarPai( antigo );
        if ( pai != null ) {
            if ( pai.esquerda == antigo ) {
                pai.esquerda = novo;
            } else {
                pai.direita = novo;
            }
        }
    }

    private No encontrarPai( No alvo ) {
        Deque<No> pilha = new ArrayDeque<>();
        if ( raiz != null ) {
            pilha.push( raiz );
        }
        while ( !pilha.isEmpty() ) {
            No atual = pilha.pop();
            if ( atual.esquerda == alvo || atual.direita == alvo ) {
                return atual;
            }
            if ( atual.esquerda != null ) {
                pilha.push( atual.esquerda );
            }
            if ( atual.direita != null ) {
                pilha.push( atual.direita );
            }
        }
        return null;
    }

    protected boolean contem( int chave ) {
        return buscarNo( chave ) != null;
    }

    protected No buscarNo( int chave ) {
        No atual = raiz;
        while ( atual != null ) {
            if ( chave < atual.chave ) {
                atual = atual.esquerda;
            } else if ( chave > atual.chave ) {
                atual = atual.direita;
            } else {
                return atual;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // controle da gravação dos passos
    // ------------------------------------------------------------------

    /**
     * Com gravação desligada a operação roda direto, sem guardar passos
     * intermediários (usado quando a animação está desligada).
     */
    public void setGravando( boolean gravando ) {
        this.gravando = gravando;
    }

    public boolean isGravando() {
        return gravando;
    }

    private void iniciar( String operacao ) {
        this.operacao = operacao;
        passos.clear();
        caminho.clear();
        comparacoes = 0;
        rotacoes = 0;
        recemCriado = null;
    }

    /**
     * Último passo: sempre é gravado, mesmo com a gravação desligada.
     */
    private void concluir( String resumo, Marca... marcas ) {
        caminho.clear();
        StringBuilder sb = new StringBuilder( resumo );
        sb.append( "  " ).append( plural( comparacoes, "comparação", "comparações" ) );
        if ( usaRotacoes() ) {
            sb.append( ", " ).append( plural( rotacoes, "rotação", "rotações" ) );
        }
        sb.append( "." );
        passos.add( criarPasso( TipoPasso.CONCLUIDO, sb.toString(), -1, -1, marcas ) );
    }

    private static String plural( int n, String singular, String plural ) {
        return n + " " + ( n == 1 ? singular : plural );
    }

    private List<Passo> pegarPassos() {
        List<Passo> copia = new ArrayList<>( passos );
        passos.clear();
        return copia;
    }

    private Passo criarPasso( TipoPasso tipo, String texto, int copiaDe, int copiaPara, Marca... marcas ) {
        Map<Integer, Destaque> destaques = new HashMap<>();
        for ( int id : caminho ) {
            destaques.put( id, Destaque.CAMINHO );
        }
        for ( Marca marca : marcas ) {
            if ( marca != null && marca.no() != null ) {
                destaques.put( marca.no().id, marca.destaque() );
            }
        }
        return new Passo( operacao, texto, tipo, NoFoto.copiar( raiz ), destaques,
                          comparacoes, rotacoes, copiaDe, copiaPara );
    }

    /**
     * Foto da árvore como está agora, fora de qualquer operação.
     */
    public Passo fotoAtual( String operacao, String texto ) {
        return new Passo( operacao, texto, TipoPasso.CONCLUIDO, NoFoto.copiar( raiz ),
                          new HashMap<>(), 0, 0, -1, -1 );
    }

    // ------------------------------------------------------------------
    // desfazer / limpar / informações
    // ------------------------------------------------------------------

    public Estado salvarEstado( String descricao ) {
        return new Estado( clonar( raiz ), proximoId, descricao );
    }

    public void restaurarEstado( Estado estado ) {
        raiz = clonar( estado.raiz );
        proximoId = estado.proximoId;
    }

    private static No clonar( No no ) {
        if ( no == null ) {
            return null;
        }
        No copia = new No( no.id, no.chave );
        copia.altura = no.altura;
        copia.vermelho = no.vermelho;
        copia.esquerda = clonar( no.esquerda );
        copia.direita = clonar( no.direita );
        return copia;
    }

    public void limpar() {
        raiz = null;
    }

    public boolean isVazia() {
        return raiz == null;
    }

    public int getTamanho() {
        return NoFoto.tamanho( NoFoto.copiar( raiz ) );
    }

    public boolean possuiChave( int chave ) {
        return contem( chave );
    }

}
