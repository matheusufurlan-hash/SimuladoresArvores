package arvores.modelo;

/**
 * Tipos de percurso (travessia) mostrados pelo simulador.
 *
 * @author Matheus Ubinha Furlan
 */
public enum Percurso {

    EM_ORDEM( "Em ordem" ),
    PRE_ORDEM( "Pré-ordem" ),
    POS_ORDEM( "Pós-ordem" ),
    EM_NIVEL( "Em nível" );

    private final String nome;

    Percurso( String nome ) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

}
