package arvores.modelo;

/**
 * Tipos de destaque que um nó pode receber em um passo da simulação.
 * O simulador escolhe a cor de cada um; aqui fica só o significado.
 *
 * @author Matheus Ubinha Furlan
 */
public enum Destaque {

    CAMINHO( "Caminho percorrido" ),
    VISITANDO( "Comparando" ),
    NOVO( "Inserido" ),
    ENCONTRADO( "Encontrado" ),
    REMOVENDO( "Será removido" ),
    SUCESSOR( "Substituto" ),
    DESBALANCEADO( "Desbalanceado" ),
    ROTACAO( "Rotação" ),
    CORES( "Troca de cor" );

    private final String descricao;

    Destaque( String descricao ) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

}
