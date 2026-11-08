package arvores;

import arvores.visual.Janelas;
import arvores.visual.Menu;
import arvores.visual.SimuladorAVL;
import arvores.visual.SimuladorBST;
import arvores.visual.SimuladorVermelhoPreto;

/**
 * Ponto de entrada do projeto Simuladores de Árvores
 * (Estruturas de Dados - IFSP São João da Boa Vista).
 *
 * Esta classe NÃO é uma janela de propósito. A EngineFrame da JSGE liga o
 * sistema gráfico do Java no instante em que é carregada (ela cria um cursor
 * num campo estático). Então a escolha do modo gráfico mais rápido
 * (Janelas.configurarGraficos) precisa acontecer antes de qualquer classe da
 * JSGE ser usada, e o main de uma subclasse da EngineFrame já seria tarde.
 *
 * Sem argumentos abre o menu. Com "bst", "avl" ou "vp" abre direto um simulador:
 *   java -jar SimuladoresArvores.jar avl
 *
 * @author Matheus Ubinha Furlan
 */
public class Main {

    public static void main( String[] args ) {

        Janelas.configurarGraficos();

        String qual = args.length > 0 ? args[0].toLowerCase() : "";
        switch ( qual ) {
            case "bst":
                new SimuladorBST();
                break;
            case "avl":
                new SimuladorAVL();
                break;
            case "vp":
            case "rb":
                new SimuladorVermelhoPreto();
                break;
            default:
                new Menu();
                break;
        }

    }

}
