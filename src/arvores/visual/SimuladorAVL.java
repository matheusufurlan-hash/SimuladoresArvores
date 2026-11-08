package arvores.visual;

import arvores.modelo.ArvoreAnimada;
import arvores.modelo.ArvoreAVL;
import arvores.modelo.NoFoto;
import arvores.modelo.Passo;
import br.com.davidbuzatto.jsge.imgui.GuiCheckBox;
import java.awt.Color;
import java.util.List;

/**
 * Simulador da Árvore AVL.
 *
 * @author Matheus Ubinha Furlan
 */
public class SimuladorAVL extends SimuladorArvore {

    private static final Color COR = new Color( 5, 150, 105 );
    private static final Color COR_NO = new Color( 16, 185, 129 );
    private static final Color COR_DESBALANCEADO = new Color( 249, 115, 22 );

    private ArvoreAVL avl;
    private GuiCheckBox chkAlturaFB;
    private GuiCheckBox chkVerificacoes;

    public SimuladorAVL() {
        super( "Simulador - Árvore AVL" );
    }

    @Override
    protected ArvoreAnimada criarArvore() {
        avl = new ArvoreAVL();
        return avl;
    }

    @Override
    protected String getTitulo() {
        return "Árvore AVL";
    }

    @Override
    protected String getSubtitulo() {
        return "Balanceada pela altura (|FB| <= 1)";
    }

    @Override
    protected Color getCorTemaSimulador() {
        return COR;
    }

    @Override
    protected int[] getExemplo() {
        // 30: rotação à esquerda (DD) | 50: DD | 25: dupla (DE) | 4: direita (EE) | 7: dupla (ED)
        return new int[]{ 10, 20, 30, 40, 50, 25, 5, 4, 7 };
    }

    @Override
    protected String getAjudaEspecifica() {
        return "# Árvore AVL\n"
             + "Altura: folha = 1 e link nulo = 0.  FB (fator de balanceamento) = altura(esquerda) - altura(direita).\n"
             + "Depois de inserir ou remover, na volta da recursão, cada nó do caminho é conferido. "
             + "Se |FB| > 1, o nó fica laranja e acontece uma rotação:\n"
             + "Esquerda-Esquerda: rotação à direita.   Direita-Direita: rotação à esquerda.\n"
             + "Esquerda-Direita: rotação dupla (esquerda no filho, direita no nó).   "
             + "Direita-Esquerda: rotação dupla (direita no filho, esquerda no nó).\n"
             + "O Exemplo (10 20 30 40 50 25 5 4 7) passa pelos quatro casos.";
    }

    @Override
    protected Color getCorNo( NoFoto no ) {
        return Math.abs( no.getFatorBalanceamento() ) > 1 ? COR_DESBALANCEADO : COR_NO;
    }

    @Override
    protected double criarControlesExtras( double x, double y, double largura ) {

        chkAlturaFB = new GuiCheckBox( x, y, largura, 16, "Mostrar altura e FB", this );
        chkAlturaFB.setSelected( true );
        chkVerificacoes = new GuiCheckBox( x, y + 22, largura, 16, "Passos de conferência do FB", this );
        chkVerificacoes.setSelected( true );
        adicionarComponente( chkAlturaFB );
        adicionarComponente( chkVerificacoes );
        adicionarDica( chkVerificacoes, "Mostra um passo para cada nó conferido na volta da recursão" );

        return y + 46;

    }

    @Override
    protected void atualizarControlesExtras() {
        avl.setMostrarVerificacoes( chkVerificacoes.isSelected() );
    }

    private static String fb( NoFoto no ) {
        int fb = no.getFatorBalanceamento();
        return fb > 0 ? "+" + fb : String.valueOf( fb );
    }

    @Override
    protected String getRotuloNo( NoFoto no ) {
        if ( !chkAlturaFB.isSelected() ) {
            return null;
        }
        return "h=" + no.altura + "  FB=" + fb( no );
    }

    @Override
    protected Color getCorRotuloNo( NoFoto no ) {
        return Math.abs( no.getFatorBalanceamento() ) > 1 ? COR_DESBALANCEADO : COR_TEXTO_SUAVE;
    }

    @Override
    protected void adicionarInfoNo( NoFoto no, List<String> linhas ) {
        boolean ok = Math.abs( no.getFatorBalanceamento() ) <= 1;
        linhas.add( "FB = " + NoFoto.altura( no.esquerda ) + " - " + NoFoto.altura( no.direita )
                    + " = " + fb( no ) + ( ok ? "  (balanceado)" : "  (DESBALANCEADO)" ) );
    }

    @Override
    protected void adicionarEstatisticas( NoFoto raiz, List<String> itens ) {
        Passo p = getPassoAtual();
        itens.add( "rotações nesta operação " + p.rotacoes );
    }

    @Override
    protected void adicionarLegendaFixa( List<ItemLegenda> itens ) {
        itens.add( new ItemLegenda( COR_NO, "|FB| <= 1" ) );
        itens.add( new ItemLegenda( COR_DESBALANCEADO, "|FB| > 1" ) );
    }

    /**
     * Abre só este simulador ("Executar Arquivo" no NetBeans). O modo gráfico
     * rápido vem do run.jvmargs do projeto; pelo terminal, use o ponto de
     * entrada: java -jar SimuladoresArvores.jar avl
     */
    public static void main( String[] args ) {
        new SimuladorAVL();
    }

}
