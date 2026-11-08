package arvores.modelo;

import java.util.Collections;
import java.util.Map;

/**
 * Um passo da simulação: a foto da árvore naquele momento, o texto que
 * explica o que está acontecendo e quais nós devem ficar destacados.
 *
 * @author Matheus Ubinha Furlan
 */
public class Passo {

    public final String operacao;     // ex.: "put(42)"
    public final String texto;
    public final TipoPasso tipo;
    public final NoFoto raiz;
    public final Map<Integer, Destaque> destaques;  // id do nó -> destaque
    public final int comparacoes;     // acumulado na operação até aqui
    public final int rotacoes;        // acumulado na operação até aqui

    // usado quando uma chave é copiada de um nó para outro (sucessor)
    public final int copiaDeId;
    public final int copiaParaId;

    public Passo( String operacao, String texto, TipoPasso tipo, NoFoto raiz,
                  Map<Integer, Destaque> destaques, int comparacoes, int rotacoes,
                  int copiaDeId, int copiaParaId ) {
        this.operacao = operacao;
        this.texto = texto;
        this.tipo = tipo;
        this.raiz = raiz;
        this.destaques = Collections.unmodifiableMap( destaques );
        this.comparacoes = comparacoes;
        this.rotacoes = rotacoes;
        this.copiaDeId = copiaDeId;
        this.copiaParaId = copiaParaId;
    }

    public Destaque getDestaque( int id ) {
        return destaques.get( id );
    }

}
