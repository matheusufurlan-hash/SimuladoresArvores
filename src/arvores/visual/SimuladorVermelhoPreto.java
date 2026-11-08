package arvores.visual;

import arvores.modelo.ArvoreAnimada;
import arvores.modelo.ArvoreVermelhoPreto;
import arvores.modelo.NoFoto;
import br.com.davidbuzatto.jsge.imgui.GuiCheckBox;
import java.awt.Color;
import java.util.List;

/**
 * Simulador da Árvore Vermelho-Preto (caída à esquerda).
 *
 * @author Matheus Ubinha Furlan
 */
public class SimuladorVermelhoPreto extends SimuladorArvore {

    private static final Color COR = new Color( 220, 38, 38 );
    private static final Color COR_VERMELHO = new Color( 229, 57, 53 );
    private static final Color COR_PRETO = new Color( 38, 50, 56 );

    private GuiCheckBox chkVisao23;
    private GuiCheckBox chkAlturaNegra;
    private boolean visao23;

    public SimuladorVermelhoPreto() {
        super( "Simulador - Árvore Vermelho-Preto" );
    }

    @Override
    protected ArvoreAnimada criarArvore() {
        return new ArvoreVermelhoPreto();
    }

    @Override
    protected String getTitulo() {
        return "Árvore Vermelho-Preto";
    }

    @Override
    protected String getSubtitulo() {
        return "Caída à esquerda (Sedgewick)";
    }

    @Override
    protected Color getCorTemaSimulador() {
        return COR;
    }

    @Override
    protected int[] getExemplo() {
        // S E A R C H X M P L (exemplo do livro), com cada letra trocada pela posição no alfabeto
        return new int[]{ 19, 5, 1, 18, 3, 8, 24, 13, 16, 12 };
    }

    @Override
    protected String getAjudaEspecifica() {
        return "# Árvore Vermelho-Preto (caída à esquerda)\n"
             + "A cor do nó é a cor do link que vem do pai. Regras: link vermelho só à esquerda; nunca dois "
             + "vermelhos seguidos; a mesma quantidade de links pretos da raiz até qualquer link nulo.\n"
             + "Na volta do put: vermelho à direita = rotação à esquerda; dois vermelhos seguidos à esquerda = "
             + "rotação à direita; dois filhos vermelhos = inversão de cores (flipColors).\n"
             + "No delete, moveRedLeft / moveRedRight garantem que a descida nunca pare em um nó-2.\n"
             + "\"Visão 2-3\" deita os links vermelhos e mostra a árvore 2-3 equivalente. "
             + "O Exemplo é o S E A R C H X M P L do livro (cada letra virou a posição dela no alfabeto).";
    }

    @Override
    protected Color getCorNo( NoFoto no ) {
        return no.vermelho ? COR_VERMELHO : COR_PRETO;
    }

    @Override
    protected Color getCorAresta( NoFoto filho ) {
        return filho.vermelho ? COR_VERMELHO : COR_ARESTA;
    }

    @Override
    protected float getLarguraAresta( NoFoto filho ) {
        return filho.vermelho ? 4.5f : 2.4f;
    }

    @Override
    protected boolean isMesmoNivel( NoFoto pai, NoFoto filho ) {
        return visao23 && filho.vermelho;
    }

    @Override
    protected double criarControlesExtras( double x, double y, double largura ) {
        chkVisao23 = new GuiCheckBox( x, y, largura, 16, "Visão 2-3 (vermelhos deitados)", this );
        chkAlturaNegra = new GuiCheckBox( x, y + 22, largura, 16, "Mostrar altura negra dos nós", this );
        adicionarComponente( chkVisao23 );
        adicionarComponente( chkAlturaNegra );
        adicionarDica( chkVisao23, "Coloca cada filho vermelho no nível do pai: os pares viram nós-3 da árvore 2-3" );
        return y + 46;
    }

    @Override
    protected void atualizarControlesExtras() {
        visao23 = chkVisao23.isSelected();
    }

    @Override
    protected String getRotuloNo( NoFoto no ) {
        return chkAlturaNegra.isSelected() ? "hn=" + NoFoto.alturaNegra( no ) : null;
    }

    @Override
    protected void adicionarInfoNo( NoFoto no, List<String> linhas ) {
        if ( no.vermelho ) {
            linhas.add( "Cor: vermelho (forma um nó-3 com o pai)" );
        } else {
            linhas.add( "Cor: preto" );
        }
        linhas.add( "Altura negra: " + NoFoto.alturaNegra( no ) + " (nós pretos até um link nulo)" );
    }

    @Override
    protected void adicionarEstatisticas( NoFoto raiz, List<String> itens ) {
        itens.add( "altura negra " + NoFoto.alturaNegra( raiz ) );
        itens.add( "rotações nesta operação " + getPassoAtual().rotacoes );
    }

    @Override
    protected void adicionarLegendaFixa( List<ItemLegenda> itens ) {
        itens.add( new ItemLegenda( COR_VERMELHO, "vermelho" ) );
        itens.add( new ItemLegenda( COR_PRETO, "preto" ) );
    }

    /**
     * Abre só este simulador ("Executar Arquivo" no NetBeans). O modo gráfico
     * rápido vem do run.jvmargs do projeto; pelo terminal, use o ponto de
     * entrada: java -jar SimuladoresArvores.jar vp
     */
    public static void main( String[] args ) {
        new SimuladorVermelhoPreto();
    }

}
