package arvores.visual;

import arvores.modelo.ArvoreAnimada;
import arvores.modelo.ArvoreBinariaBusca;
import arvores.modelo.NoFoto;
import br.com.davidbuzatto.jsge.imgui.GuiButtonGroup;
import br.com.davidbuzatto.jsge.imgui.GuiCheckBox;
import br.com.davidbuzatto.jsge.imgui.GuiLabel;
import br.com.davidbuzatto.jsge.imgui.GuiRadioButton;
import java.awt.Color;
import java.util.List;

/**
 * Simulador da Árvore Binária de Busca.
 *
 * @author Matheus Ubinha Furlan
 */
public class SimuladorBST extends SimuladorArvore {

    private static final Color COR = new Color( 37, 99, 235 );
    private static final Color COR_NO = new Color( 59, 130, 246 );

    private ArvoreBinariaBusca bst;
    private GuiRadioButton rdoFusao;
    private GuiRadioButton rdoHibbard;
    private GuiCheckBox chkAltura;

    public SimuladorBST() {
        super( "Simulador - Árvore Binária de Busca" );
    }

    @Override
    protected ArvoreAnimada criarArvore() {
        bst = new ArvoreBinariaBusca();
        return bst;
    }

    @Override
    protected String getTitulo() {
        return "Árvore Binária de Busca";
    }

    @Override
    protected String getSubtitulo() {
        return "Menores à esquerda, maiores à direita";
    }

    @Override
    protected Color getCorTemaSimulador() {
        return COR;
    }

    @Override
    protected int[] getExemplo() {
        return new int[]{ 50, 30, 70, 20, 40, 60, 80, 35, 45, 65 };
    }

    @Override
    protected String getAjudaEspecifica() {
        return "# Árvore Binária de Busca\n"
             + "Em cada nó, as chaves menores ficam à esquerda e as maiores à direita. Não há balanceamento: "
             + "gere uma árvore \"Crescente\" para ver ela virar uma lista (altura = número de nós).\n"
             + "# Remoção de nó com dois filhos (escolha em Exibição)\n"
             + "Fusão (como na biblioteca da aula): a subárvore direita sobe para o lugar do nó e a "
             + "subárvore esquerda é pendurada no menor nó da direita.\n"
             + "Hibbard (livro do Sedgewick): o sucessor (menor da direita) sai do lugar dele e ocupa o lugar do nó removido.";
    }

    @Override
    protected Color getCorNo( NoFoto no ) {
        return COR_NO;
    }

    @Override
    protected double criarControlesExtras( double x, double y, double largura ) {

        chkAltura = new GuiCheckBox( x, y, largura, 16, "Mostrar altura dos nós", this );
        adicionarComponente( chkAltura );

        GuiLabel rotulo = new GuiLabel( x, y + 24, largura, 16, "Remoção de nó com 2 filhos:", this );
        adicionarComponente( rotulo );

        GuiButtonGroup grupo = new GuiButtonGroup();
        rdoFusao = new GuiRadioButton( x + 8, y + 44, largura - 8, 16, "Fusão (como na aula)", this );
        rdoHibbard = new GuiRadioButton( x + 8, y + 64, largura - 8, 16, "Hibbard (sucessor)", this );
        rdoFusao.setButtonGroup( grupo );
        rdoHibbard.setButtonGroup( grupo );
        rdoFusao.setSelected( true );
        adicionarComponente( rdoFusao );
        adicionarComponente( rdoHibbard );
        adicionarDica( rdoFusao, "Igual à BinarySearchTree da biblioteca aesd" );
        adicionarDica( rdoHibbard, "Igual ao livro: o sucessor sai do lugar dele e ocupa o do nó removido" );

        return y + 88;

    }

    @Override
    protected void atualizarControlesExtras() {
        bst.setModoRemocao( rdoHibbard.isSelected()
                ? ArvoreBinariaBusca.ModoRemocao.HIBBARD
                : ArvoreBinariaBusca.ModoRemocao.FUSAO );
    }

    @Override
    protected String getRotuloNo( NoFoto no ) {
        return chkAltura.isSelected() ? "h=" + no.altura : null;
    }

    @Override
    protected void adicionarEstatisticas( NoFoto raiz, List<String> itens ) {
        int n = NoFoto.tamanho( raiz );
        if ( n > 2 && NoFoto.altura( raiz ) == n ) {
            itens.add( "degenerada (virou lista)" );
        }
    }

    /**
     * Abre só este simulador ("Executar Arquivo" no NetBeans). O modo gráfico
     * rápido vem do run.jvmargs do projeto; pelo terminal, use o ponto de
     * entrada: java -jar SimuladoresArvores.jar bst
     */
    public static void main( String[] args ) {
        new SimuladorBST();
    }

}
