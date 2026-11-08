package arvores.visual;

import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/**
 * Controla as janelas abertas.
 *
 * Cada simulador roda na sua própria janela (e na sua própria thread, que
 * a JSGE cria). Fechar uma janela não fecha as outras; o programa só
 * termina quando a última janela é fechada.
 *
 * @author Matheus Ubinha Furlan
 */
public final class Janelas {

    private static int abertas = 0;
    private static int simuladoresCriados = 0;

    private Janelas() {
    }

    /**
     * Escolhe como o Java vai desenhar na tela. Tem que ser chamado antes de
     * qualquer classe da JSGE ser carregada (ver arvores.Main).
     *
     * No Linux o Java usa, por padrão, o pipeline "XRender": cada forma com
     * antialiasing vira um pedido ao servidor gráfico (Xorg). Medido nesta
     * máquina: cada quadro levava ~115 ms (uns 9 quadros por segundo) e o Xorg
     * ficava com 125% de CPU. Com o pipeline X11 e o buffer de desenho na
     * memória do próprio Java, o desenho é feito pelo Java e enviado pronto:
     * 60 quadros por segundo. Em outros sistemas nada muda.
     *
     * Se alguém passar essas opções na linha de comando, elas são respeitadas.
     */
    public static void configurarGraficos() {
        String sistema = System.getProperty( "os.name", "" ).toLowerCase();
        if ( sistema.contains( "linux" ) ) {
            if ( System.getProperty( "sun.java2d.xrender" ) == null ) {
                System.setProperty( "sun.java2d.xrender", "false" );
            }
            if ( System.getProperty( "sun.java2d.pmoffscreen" ) == null ) {
                System.setProperty( "sun.java2d.pmoffscreen", "false" );
            }
        }
    }

    /**
     * Registra a janela do menu (fica centralizada).
     */
    public static synchronized void registrarMenu( JFrame janela ) {
        registrar( janela );
    }

    /**
     * Registra a janela de um simulador e a desloca um pouco (em cascata)
     * para que várias janelas abertas não fiquem exatamente uma em cima da outra.
     */
    public static synchronized void registrarSimulador( JFrame janela ) {
        int deslocamento = ( simuladoresCriados % 3 ) * 36 - 36;
        simuladoresCriados++;
        Point p = janela.getLocation();
        janela.setLocation( Math.max( 0, p.x + deslocamento ), Math.max( 0, p.y + deslocamento ) );
        registrar( janela );
    }

    private static void registrar( JFrame janela ) {
        janela.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        abertas++;
        janela.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosed( WindowEvent e ) {
                janelaFechada();
            }
        });
    }

    private static synchronized void janelaFechada() {
        abertas--;
        if ( abertas <= 0 ) {
            System.exit( 0 );
        }
    }

}
