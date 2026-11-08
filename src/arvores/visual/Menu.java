package arvores.visual;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.core.utils.ColorUtils;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiComponent;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Menu inicial: abre cada simulador em uma janela independente.
 * Dá para abrir os três ao mesmo tempo (e até mais de uma vez cada um).
 *
 * Quem abre o menu é o arvores.Main (ponto de entrada do projeto).
 *
 * @author Matheus Ubinha Furlan
 */
public class Menu extends EngineFrame {

    private static final Color COR_FUNDO = new Color( 243, 245, 249 );
    private static final Color COR_TEXTO = new Color( 30, 41, 59 );
    private static final Color COR_SUAVE = new Color( 100, 116, 139 );
    private static final Color COR_BST = new Color( 37, 99, 235 );
    private static final Color COR_AVL = new Color( 5, 150, 105 );
    private static final Color COR_RB = new Color( 220, 38, 38 );

    private List<GuiComponent> componentes;
    private GuiButton btnBST;
    private GuiButton btnAVL;
    private GuiButton btnRB;
    private GuiButton btnTodos;

    public Menu() {
        super(
            600,                       // largura
            480,                       // altura
            "Simuladores de Árvores",  // título
            30,                        // quadros por segundo (o menu quase não muda)
            true                       // suavização
        );
    }

    @Override
    public void create() {

        Janelas.registrarMenu( this );
        setExitKey( KEY_NULL );

        componentes = new ArrayList<>();
        btnBST = new GuiButton( 150, 112, 410, 56, "", this );
        btnAVL = new GuiButton( 150, 196, 410, 56, "", this );
        btnRB = new GuiButton( 150, 280, 410, 56, "", this );
        btnTodos = new GuiButton( 40, 372, 520, 40, "Abrir os três simuladores", this );
        estilizar( btnBST, COR_BST );
        estilizar( btnAVL, COR_AVL );
        estilizar( btnRB, COR_RB );
        componentes.add( btnBST );
        componentes.add( btnAVL );
        componentes.add( btnRB );
        componentes.add( btnTodos );

    }

    private void estilizar( GuiButton botao, Color cor ) {
        botao.setBackgroundColor( cor );
        botao.setBorderColor( ColorUtils.colorBrightness( cor, -0.25 ) );
        botao.setTextColor( Color.WHITE );
    }

    @Override
    public void update( double delta ) {

        for ( GuiComponent c : componentes ) {
            c.update( delta );
        }

        if ( btnBST.isMousePressed() ) {
            SwingUtilities.invokeLater( SimuladorBST::new );
        }
        if ( btnAVL.isMousePressed() ) {
            SwingUtilities.invokeLater( SimuladorAVL::new );
        }
        if ( btnRB.isMousePressed() ) {
            SwingUtilities.invokeLater( SimuladorVermelhoPreto::new );
        }
        if ( btnTodos.isMousePressed() ) {
            SwingUtilities.invokeLater( () -> {
                new SimuladorBST();
                new SimuladorAVL();
                new SimuladorVermelhoPreto();
            });
        }

    }

    @Override
    public void draw() {

        clearBackground( COR_FUNDO );
        fillRectangle( 0, 0, getScreenWidth(), 6, COR_BST );
        fillRectangle( getScreenWidth() / 3.0, 0, getScreenWidth() / 3.0, 6, COR_AVL );
        fillRectangle( getScreenWidth() * 2 / 3.0, 0, getScreenWidth() / 3.0, 6, COR_RB );

        texto( "Simuladores de Árvores", 40, 30, 28, Font.BOLD, COR_TEXTO );
        texto( "put  ·  delete  ·  get   —   passo a passo, com animação", 40, 70, 14, Font.PLAIN, COR_SUAVE );

        icone( 95, 140, COR_BST, false );
        icone( 95, 224, COR_AVL, false );
        icone( 95, 308, COR_RB, true );

        setFontName( Font.MONOSPACED );
        setFontStyle( Font.BOLD );
        for ( GuiComponent c : componentes ) {
            c.draw();
        }

        rotular( btnBST, "Árvore Binária de Busca", "Sem balanceamento  ·  remoção por fusão ou Hibbard" );
        rotular( btnAVL, "Árvore AVL", "Rotações guiadas pelo fator de balanceamento" );
        rotular( btnRB, "Árvore Vermelho-Preto", "Caída à esquerda  ·  rotações e troca de cores" );

        texto( "Cada simulador abre em uma janela independente.", 40, 424, 12, Font.PLAIN, COR_SUAVE );
        texto( "Estruturas de Dados  ·  IFSP São João da Boa Vista  ·  JSGE", 40, 446, 12, Font.PLAIN, COR_SUAVE );

    }

    /**
     * Título e descrição desenhados por cima do botão.
     */
    private void rotular( GuiButton botao, String titulo, String descricao ) {
        boolean sobre = botao.isMouseOver() || botao.isMouseDown();
        Color c1 = sobre ? COR_TEXTO : Color.WHITE;
        Color c2 = sobre ? COR_SUAVE : ColorUtils.fade( Color.WHITE, 0.85 );
        texto( titulo, botao.getX() + 18, botao.getY() + 10, 17, Font.BOLD, c1 );
        texto( descricao, botao.getX() + 18, botao.getY() + 34, 12, Font.PLAIN, c2 );
    }

    /**
     * Desenho de uma arvorezinha de 3 nós.
     */
    private void icone( double x, double y, Color cor, boolean vermelhoPreto ) {
        Color clara = ColorUtils.colorBrightness( cor, 0.25 );
        Color filhoEsq = vermelhoPreto ? new Color( 229, 57, 53 ) : clara;
        Color raiz = vermelhoPreto ? new Color( 38, 50, 56 ) : cor;
        Color filhoDir = vermelhoPreto ? new Color( 38, 50, 56 ) : clara;
        setStrokeLineWidth( 3 );
        drawLine( x, y - 12, x - 26, y + 14, vermelhoPreto ? filhoEsq : ColorUtils.colorBrightness( cor, -0.1 ) );
        drawLine( x, y - 12, x + 26, y + 14, ColorUtils.colorBrightness( cor, -0.1 ) );
        fillCircle( x, y - 12, 14, raiz );
        fillCircle( x - 26, y + 14, 11, filhoEsq );
        fillCircle( x + 26, y + 14, 11, filhoDir );
    }

    private void texto( String t, double x, double y, int tamanho, int estilo, Color cor ) {
        setFontName( Font.SANS_SERIF );
        setFontStyle( estilo );
        drawText( t, x, y, tamanho, cor );
    }

}
