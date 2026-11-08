package arvores.visual;

import arvores.modelo.ArvoreAnimada;
import arvores.modelo.Destaque;
import arvores.modelo.NoFoto;
import arvores.modelo.Passo;
import arvores.modelo.Percurso;
import arvores.modelo.TipoPasso;
import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.core.utils.ColorUtils;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiCheckBox;
import br.com.davidbuzatto.jsge.imgui.GuiComponent;
import br.com.davidbuzatto.jsge.imgui.GuiConfirmDialog;
import br.com.davidbuzatto.jsge.imgui.GuiDropdownList;
import br.com.davidbuzatto.jsge.imgui.GuiGroup;
import br.com.davidbuzatto.jsge.imgui.GuiProgressBar;
import br.com.davidbuzatto.jsge.imgui.GuiSlider;
import br.com.davidbuzatto.jsge.imgui.GuiSpinner;
import br.com.davidbuzatto.jsge.imgui.GuiTextField;
import br.com.davidbuzatto.jsge.imgui.GuiToolTip;
import br.com.davidbuzatto.jsge.image.Image;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Base dos três simuladores: monta a interface (ImGUI da JSGE), executa
 * as operações na árvore, reproduz os passos com animação e desenha.
 *
 * Como funciona a animação:
 * 1. cada operação (put/delete/get) devolve uma lista de passos;
 * 2. cada passo tem uma "foto" da árvore;
 * 3. a tela sempre mostra a transição entre a foto anterior e a atual:
 *    os nós são identificados pelo id, então um nó que mudou de lugar
 *    (rotação, remoção...) desliza da posição antiga para a nova.
 *
 * Atenção (JSGE): o create() roda antes dos inicializadores de campo da
 * subclasse. Por isso nenhum campo é inicializado na declaração: tudo é
 * preparado dentro do create().
 *
 * @author Matheus Ubinha Furlan
 */
public abstract class SimuladorArvore extends EngineFrame {

    // ------------------------------------------------------------------
    // constantes
    // ------------------------------------------------------------------

    protected static final int LARGURA_PAINEL = 300;
    protected static final double RAIO = 19;

    private static final double ESPACO_X_MAXIMO = 46;
    private static final double ESPACO_X_MINIMO = 28;
    private static final double ESPACO_X_COM_ROTULO = 36;
    private static final double ESPACO_Y = 76;
    private static final double ESCALA_MAXIMA = 1.35;
    private static final int MAXIMO_NOS = 63;
    private static final int CHAVE_MAXIMA = 999;
    private static final double TEMPO_TRANSICAO = 0.5;
    private static final double TEMPO_PAUSA = 0.5;

    protected static final Color COR_FUNDO = new Color( 243, 245, 249 );
    protected static final Color COR_GRADE = new Color( 232, 236, 243 );
    protected static final Color COR_PAINEL = Color.WHITE;
    protected static final Color COR_BORDA = new Color( 220, 225, 233 );
    protected static final Color COR_TEXTO = new Color( 30, 41, 59 );
    protected static final Color COR_TEXTO_SUAVE = new Color( 100, 116, 139 );
    protected static final Color COR_ARESTA = new Color( 148, 163, 184 );
    protected static final Color COR_NULO = new Color( 176, 186, 201 );

    private static final Map<Destaque, Color> CORES_DESTAQUE = new EnumMap<>( Destaque.class );
    private static final Map<TipoPasso, Color> CORES_PASSO = new EnumMap<>( TipoPasso.class );

    static {
        CORES_DESTAQUE.put( Destaque.CAMINHO, new Color( 251, 191, 36 ) );
        CORES_DESTAQUE.put( Destaque.VISITANDO, new Color( 245, 158, 11 ) );
        CORES_DESTAQUE.put( Destaque.NOVO, new Color( 34, 197, 94 ) );
        CORES_DESTAQUE.put( Destaque.ENCONTRADO, new Color( 22, 163, 74 ) );
        CORES_DESTAQUE.put( Destaque.REMOVENDO, new Color( 239, 68, 68 ) );
        CORES_DESTAQUE.put( Destaque.SUCESSOR, new Color( 6, 182, 212 ) );
        CORES_DESTAQUE.put( Destaque.DESBALANCEADO, new Color( 249, 115, 22 ) );
        CORES_DESTAQUE.put( Destaque.ROTACAO, new Color( 139, 92, 246 ) );
        CORES_DESTAQUE.put( Destaque.CORES, new Color( 236, 72, 153 ) );

        CORES_PASSO.put( TipoPasso.INFO, new Color( 59, 130, 246 ) );
        CORES_PASSO.put( TipoPasso.SUCESSO, new Color( 34, 197, 94 ) );
        CORES_PASSO.put( TipoPasso.AVISO, new Color( 245, 158, 11 ) );
        CORES_PASSO.put( TipoPasso.ERRO, new Color( 239, 68, 68 ) );
        CORES_PASSO.put( TipoPasso.ROTACAO, new Color( 139, 92, 246 ) );
        CORES_PASSO.put( TipoPasso.AJUSTE, new Color( 236, 72, 153 ) );
        CORES_PASSO.put( TipoPasso.CONCLUIDO, new Color( 71, 85, 105 ) );
    }

    private enum TipoOperacao {
        INSERIR, REMOVER, BUSCAR
    }

    private record Operacao( TipoOperacao tipo, int chave ) {
        String descricao() {
            switch ( tipo ) {
                case INSERIR: return "put(" + chave + ")";
                case REMOVER: return "delete(" + chave + ")";
                default: return "get(" + chave + ")";
            }
        }
    }

    /**
     * Item fixo da legenda (ex.: nó vermelho / nó preto).
     */
    protected record ItemLegenda( Color cor, String texto ) {}

    // chaves dos caches de imagens (ver CacheImagens)
    private record ChaveNo( int cor, int raio4 ) {}
    private record ChaveHalo( Destaque destaque, int raio4 ) {}
    private record ChaveTexto( String texto, int tamanho, int cor, int fundo, int borda, int extra ) {}
    private record ChavePainel( String a, String b, int cor, int largura, int altura ) {}

    // ------------------------------------------------------------------
    // estado (nada é inicializado aqui: ver create())
    // ------------------------------------------------------------------

    protected ArvoreAnimada arvore;
    protected Color corTema;

    private List<Passo> passos;
    private int indice;
    private Passo anterior;
    private Passo atual;
    private double progresso;
    private double espera;
    private boolean tocando;
    private double velocidade;
    private Deque<Operacao> fila;
    private Deque<ArvoreAnimada.Estado> historico;
    private Random sorteio;

    // câmera
    private boolean cameraIniciada;
    private double camX;
    private double camY;
    private double camEscala;
    private double zoom;
    private double panX;
    private double panY;
    private boolean arrastando;
    private double arrasteX;
    private double arrasteY;

    // interação com a árvore
    private volatile Map<Integer, double[]> posicoesTela;   // escrito no draw(), lido no update()
    private int idSobMouse;
    private boolean campoFocado;

    // imagens já desenhadas (nós, destaques, rótulos, pílulas e painéis)
    private CacheImagens cacheNos;
    private CacheImagens cacheHalos;
    private CacheImagens cacheRotulos;
    private CacheImagens cachePilulas;
    private CacheImagens cachePaineis;
    private int tamanhoTitulo;          // calculado uma vez, no primeiro draw()
    private double velocidadeMostrada;  // o texto da velocidade só é refeito quando ela muda
    private String textoVelocidade;

    // mensagem temporária
    private String toast;
    private Color corToast;
    private double tempoToast;

    // componentes ImGUI
    private List<GuiGroup> grupos;
    private List<GuiComponent> componentes;
    private List<GuiToolTip> dicas;
    private GuiTextField txtChaves;
    private GuiButton btnInserir;
    private GuiButton btnRemover;
    private GuiButton btnBuscar;
    private GuiButton btnAleatoria;
    private GuiSpinner spnQuantidade;
    private GuiButton btnGerarAleatoria;
    private GuiButton btnGerarCrescente;
    private GuiButton btnExemplo;
    private GuiButton btnDesfazer;
    private GuiButton btnLimpar;
    private GuiButton btnInicio;
    private GuiButton btnVoltar;
    private GuiButton btnPlay;
    private GuiButton btnAvancar;
    private GuiButton btnFim;
    private GuiProgressBar barraProgresso;
    private GuiSlider sldVelocidade;
    private GuiCheckBox chkPausarOperacao;
    private GuiCheckBox chkAnimar;
    private GuiCheckBox chkNulos;
    private GuiDropdownList lstPercurso;
    private GuiButton btnCentralizar;
    private GuiButton btnAjuda;
    private GuiConfirmDialog dlgLimpar;
    private GuiButton btnFecharAjuda;
    private boolean ajudaVisivel;
    private double[] quadroAjudaDesenhado;   // calculado no draw(), lido no update()
    private double espacoX;
    private double yRotuloProgresso;
    private double yRotuloVelocidade;
    private double yFimPainel;

    public SimuladorArvore( String tituloJanela ) {
        super(
            1280,           // largura
            720,            // altura
            tituloJanela,   // título
            60,             // quadros por segundo
            true,           // suavização
            true,           // redimensionável
            false,          // tela cheia
            false,          // sem decoração
            false,          // sempre no topo
            false           // fundo invisível
        );
    }

    // ------------------------------------------------------------------
    // o que cada simulador precisa dizer
    // ------------------------------------------------------------------

    protected abstract ArvoreAnimada criarArvore();

    protected abstract String getTitulo();

    protected abstract String getSubtitulo();

    protected abstract Color getCorTemaSimulador();

    protected abstract int[] getExemplo();

    protected abstract String getAjudaEspecifica();

    protected abstract Color getCorNo( NoFoto no );

    // ganchos opcionais

    /**
     * Cria os controles próprios do simulador no grupo "Exibição".
     * Devolve o y logo abaixo do último controle criado.
     */
    protected double criarControlesExtras( double x, double y, double largura ) {
        return y;
    }

    protected void atualizarControlesExtras() {
    }

    protected Color getCorAresta( NoFoto filho ) {
        return COR_ARESTA;
    }

    protected float getLarguraAresta( NoFoto filho ) {
        return 2.4f;
    }

    /**
     * Na visão 2-3 da Vermelho-Preto o filho vermelho fica no mesmo nível do pai.
     */
    protected boolean isMesmoNivel( NoFoto pai, NoFoto filho ) {
        return false;
    }

    /**
     * Texto pequeno desenhado embaixo do nó (ex.: altura e FB na AVL).
     */
    protected String getRotuloNo( NoFoto no ) {
        return null;
    }

    protected Color getCorRotuloNo( NoFoto no ) {
        return COR_TEXTO_SUAVE;
    }

    protected void adicionarInfoNo( NoFoto no, List<String> linhas ) {
    }

    protected void adicionarEstatisticas( NoFoto raiz, List<String> itens ) {
    }

    protected void adicionarLegendaFixa( List<ItemLegenda> itens ) {
    }

    /**
     * Registra um componente criado pela subclasse para ser atualizado e desenhado.
     */
    protected void adicionarComponente( GuiComponent componente ) {
        componentes.add( componente );
    }

    protected void adicionarDica( GuiComponent componente, String texto ) {
        dicas.add( new GuiToolTip( componente, texto, this ) );
    }

    /**
     * Mostra de novo a foto atual (usado quando uma opção de exibição muda).
     */
    protected Passo getPassoAtual() {
        return atual;
    }

    // ------------------------------------------------------------------
    // criação
    // ------------------------------------------------------------------

    @Override
    public void create() {

        Janelas.registrarSimulador( this );
        setExitKey( KEY_NULL );     // ESC não fecha a janela sem querer
        setMinimumSize( new Dimension( 1000, 640 ) );

        // monitor pequeno (ex.: 1366x768): a janela não cabe, então abre maximizada
        Rectangle util = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if ( getWidth() > util.width || getHeight() > util.height ) {
            setExtendedState( MAXIMIZED_BOTH );
        }

        corTema = getCorTemaSimulador();
        arvore = criarArvore();
        sorteio = new Random();
        fila = new ArrayDeque<>();
        historico = new ArrayDeque<>();
        posicoesTela = new HashMap<>();
        idSobMouse = -1;
        velocidade = 1;
        tocando = true;
        zoom = 1;
        panX = 0;
        panY = 0;
        camEscala = 1;
        cameraIniciada = false;
        espacoX = ESPACO_X_MAXIMO;
        ajudaVisivel = false;
        toast = null;
        tempoToast = 0;

        cacheNos = new CacheImagens( 300 );
        cacheHalos = new CacheImagens( 200 );
        cacheRotulos = new CacheImagens( 400 );
        cachePilulas = new CacheImagens( 100 );
        cachePaineis = new CacheImagens( 16 );
        tamanhoTitulo = 0;
        velocidadeMostrada = -1;
        textoVelocidade = "";

        Passo inicial = arvore.fotoAtual( "",
                "Digite uma chave (ou várias: 50 30 70) e clique em Inserir. Ou clique em Exemplo." );
        passos = new ArrayList<>();
        passos.add( inicial );
        indice = 0;
        anterior = inicial;
        atual = inicial;
        progresso = 1;
        espera = 0;

        criarComponentes();

    }

    private void criarComponentes() {

        grupos = new ArrayList<>();
        componentes = new ArrayList<>();
        dicas = new ArrayList<>();

        double x = 14;
        double w = LARGURA_PAINEL - 28;
        double xi = x + 12;
        double wi = w - 24;
        double meia = ( wi - 6 ) / 2;
        double y = 72;

        btnAjuda = new GuiButton( LARGURA_PAINEL - 44, 14, 30, 28, "?", this );
        componentes.add( btnAjuda );
        adicionarDica( btnAjuda, "Como usar o simulador" );

        // ---- operações ----
        grupos.add( new GuiGroup( x, y, w, 130, "Operações", this ) );
        txtChaves = new GuiTextField( xi, y + 18, wi, 28, "", this );
        btnInserir = new GuiButton( xi, y + 52, meia, 30, "Inserir (put)", this );
        btnRemover = new GuiButton( xi + meia + 6, y + 52, meia, 30, "Remover (delete)", this );
        btnBuscar = new GuiButton( xi, y + 88, meia, 30, "Buscar (get)", this );
        btnAleatoria = new GuiButton( xi + meia + 6, y + 88, meia, 30, "Aleatória", this );
        componentes.add( txtChaves );
        componentes.add( btnInserir );
        componentes.add( btnRemover );
        componentes.add( btnBuscar );
        componentes.add( btnAleatoria );
        destacarBotao( btnInserir );
        adicionarDica( btnInserir, "Insere as chaves digitadas (Enter também insere)" );
        adicionarDica( btnRemover, "Remove as chaves digitadas (clique em um nó para pegar a chave)" );
        adicionarDica( btnBuscar, "Procura a chave mostrando o caminho" );
        adicionarDica( btnAleatoria, "Insere uma chave aleatória que ainda não está na árvore" );
        y += 130 + 10;

        // ---- gerar ----
        grupos.add( new GuiGroup( x, y, w, 94, "Gerar / editar", this ) );
        spnQuantidade = new GuiSpinner( xi, y + 18, 72, 28, 10, 1, 40, this );
        btnGerarAleatoria = new GuiButton( xi + 78, y + 18, 82, 28, "Aleatória", this );
        btnGerarCrescente = new GuiButton( xi + 166, y + 18, wi - 166, 28, "Crescente", this );
        btnExemplo = new GuiButton( xi, y + 54, 80, 28, "Exemplo", this );
        btnDesfazer = new GuiButton( xi + 84, y + 54, 80, 28, "Desfazer", this );
        btnLimpar = new GuiButton( xi + 168, y + 54, wi - 168, 28, "Limpar", this );
        componentes.add( spnQuantidade );
        componentes.add( btnGerarAleatoria );
        componentes.add( btnGerarCrescente );
        componentes.add( btnExemplo );
        componentes.add( btnDesfazer );
        componentes.add( btnLimpar );
        adicionarDica( spnQuantidade, "Quantidade de chaves para gerar" );
        adicionarDica( btnGerarAleatoria, "Nova árvore com N chaves aleatórias (direto, sem animação)" );
        adicionarDica( btnGerarCrescente, "Nova árvore inserindo 10, 20, 30... em ordem (direto)" );
        adicionarDica( btnExemplo, "Nova árvore com uma sequência que mostra os casos importantes (animada)" );
        adicionarDica( btnDesfazer, "Volta a árvore para antes da última operação" );
        adicionarDica( btnLimpar, "Remove todos os nós" );
        y += 94 + 10;

        // ---- animação ----
        grupos.add( new GuiGroup( x, y, w, 156, "Animação", this ) );
        btnInicio = new GuiButton( xi, y + 18, 40, 28, "|<", this );
        btnVoltar = new GuiButton( xi + 44, y + 18, 40, 28, "<", this );
        btnPlay = new GuiButton( xi + 88, y + 18, wi - 176, 28, "Pausar", this );
        btnAvancar = new GuiButton( xi + wi - 84, y + 18, 40, 28, ">", this );
        btnFim = new GuiButton( xi + wi - 40, y + 18, 40, 28, ">|", this );
        barraProgresso = new GuiProgressBar( xi, y + 52, wi, 10, 1, 0, 1, this );
        yRotuloProgresso = y + 65;
        yRotuloVelocidade = y + 89;
        sldVelocidade = new GuiSlider( xi + 112, y + 85, wi - 112, 20, 0, -2, 2, this );
        chkPausarOperacao = new GuiCheckBox( xi, y + 112, wi, 16, "Pausar no início de cada operação", this );
        chkAnimar = new GuiCheckBox( xi, y + 132, wi, 16, "Animar passo a passo", this );
        chkAnimar.setSelected( true );
        componentes.add( btnInicio );
        componentes.add( btnVoltar );
        componentes.add( btnPlay );
        componentes.add( btnAvancar );
        componentes.add( btnFim );
        componentes.add( barraProgresso );
        componentes.add( sldVelocidade );
        componentes.add( chkPausarOperacao );
        componentes.add( chkAnimar );
        adicionarDica( btnInicio, "Primeiro passo da operação" );
        adicionarDica( btnVoltar, "Passo anterior" );
        adicionarDica( btnAvancar, "Próximo passo" );
        adicionarDica( btnFim, "Último passo da operação" );
        adicionarDica( chkPausarOperacao, "Para no primeiro passo de cada operação (bom para explicar)" );
        adicionarDica( chkAnimar, "Desligado: as operações são aplicadas direto, sem os passos" );
        y += 156 + 10;

        // ---- exibição ----
        // a lista de percursos abre para baixo, então fica no topo do grupo
        double yGrupo = y;
        List<String> percursos = new ArrayList<>();
        for ( Percurso p : Percurso.values() ) {
            percursos.add( p.getNome() );
        }
        lstPercurso = new GuiDropdownList( xi, y + 18, 150, 26, percursos, this );
        btnCentralizar = new GuiButton( xi + 156, y + 18, wi - 156, 26, "Centralizar", this );
        chkNulos = new GuiCheckBox( xi, y + 54, wi, 16, "Mostrar links nulos", this );
        componentes.add( btnCentralizar );
        componentes.add( chkNulos );
        adicionarDica( btnCentralizar, "Volta o zoom e a posição da árvore ao normal" );
        adicionarDica( lstPercurso, "Percurso mostrado na barra acima da explicação" );
        double yy = criarControlesExtras( xi, y + 76, wi );
        componentes.add( lstPercurso );   // por último: a lista aberta fica por cima
        double alturaGrupo = yy + 2 - yGrupo;
        grupos.add( new GuiGroup( x, yGrupo, w, alturaGrupo, "Exibição", this ) );
        yFimPainel = yGrupo + alturaGrupo;

        // ---- diálogos ----
        dlgLimpar = new GuiConfirmDialog( "Limpar árvore",
                "Remover todos os nós da árvore?\n(Dá para voltar com Desfazer.)",
                "Limpar", "Cancelar", "", true, this );
        btnFecharAjuda = new GuiButton( 0, 0, 110, 32, "Entendi", this );
        destacarBotao( btnFecharAjuda );

    }

    private void destacarBotao( GuiButton botao ) {
        botao.setBackgroundColor( corTema );
        botao.setBorderColor( ColorUtils.colorBrightness( corTema, -0.25 ) );
        botao.setTextColor( Color.WHITE );
    }

    /**
     * Texto da ajuda. Linhas que começam com "#" são títulos;
     * as outras são parágrafos (quebrados automaticamente na largura do quadro).
     */
    private String textoAjuda() {
        return "# Operações\n"
             + "Digite uma chave de 0 a 999, ou várias separadas por espaço (ex.: 50 30 70), e clique "
             + "em Inserir (put), Remover (delete) ou Buscar (get). Enter também insere. "
             + "Clicar em um nó copia a chave dele para o campo.\n"
             + "# Animação\n"
             + "Cada operação é mostrada passo a passo, com a explicação na caixa de baixo. "
             + "Os botões de navegação vão para o primeiro, o anterior, o próximo e o último passo; "
             + "Pausar / Tocar / Repetir controlam a reprodução e o controle deslizante muda a velocidade. "
             + "Teclado (fora do campo): Espaço, setas, Home e End. Roda do mouse: zoom; arrastar o fundo: mover.\n"
             + getAjudaEspecifica();
    }

    // ------------------------------------------------------------------
    // atualização
    // ------------------------------------------------------------------

    @Override
    public void update( double delta ) {

        boolean modal = dlgLimpar.isVisible() || ajudaVisivel;

        if ( modal ) {

            if ( ajudaVisivel ) {
                btnFecharAjuda.update( delta );
                double[] q = quadroAjudaDesenhado;
                boolean cliqueFora = q != null && isMouseButtonPressed( MOUSE_BUTTON_LEFT )
                        && ( getMouseX() < q[0] || getMouseX() > q[0] + q[2]
                          || getMouseY() < q[1] || getMouseY() > q[1] + q[3] );
                if ( btnFecharAjuda.isMousePressed() || isKeyPressed( KEY_ESCAPE ) || cliqueFora ) {
                    ajudaVisivel = false;
                    quadroAjudaDesenhado = null;
                }
            }

            if ( dlgLimpar.isVisible() ) {
                dlgLimpar.update( delta );
                if ( dlgLimpar.isButton1Pressed() ) {
                    dlgLimpar.hide();
                    limpar();
                } else if ( dlgLimpar.isButton2Pressed() || dlgLimpar.isCloseButtonPressed() ) {
                    dlgLimpar.hide();
                }
            }

        } else if ( lstPercurso.isDropdownListVisible() ) {

            // com a lista aberta, só ela recebe o mouse (o clique não "vaza")
            lstPercurso.update( delta );

        } else {

            for ( GuiGroup g : grupos ) {
                g.update( delta );
            }
            for ( GuiComponent c : componentes ) {
                c.update( delta );
            }
            for ( GuiToolTip d : dicas ) {
                d.update( delta );
            }

            tratarBotoes();
            atualizarControlesExtras();
            tratarTeclado();
            tratarMouse();

        }

        atualizarAnimacao( delta );
        atualizarCamera( delta );
        atualizarEstadoInterface();

        if ( tempoToast > 0 ) {
            tempoToast -= delta;
        }

    }

    private void tratarBotoes() {

        if ( btnInserir.isMousePressed() ) {
            operarCampo( TipoOperacao.INSERIR );
        }
        if ( btnRemover.isMousePressed() ) {
            operarCampo( TipoOperacao.REMOVER );
        }
        if ( btnBuscar.isMousePressed() ) {
            operarCampo( TipoOperacao.BUSCAR );
        }
        if ( btnAleatoria.isMousePressed() ) {
            inserirAleatoria();
        }
        if ( btnGerarAleatoria.isMousePressed() ) {
            gerar( false );
        }
        if ( btnGerarCrescente.isMousePressed() ) {
            gerar( true );
        }
        if ( btnExemplo.isMousePressed() ) {
            carregarExemplo();
        }
        if ( btnDesfazer.isMousePressed() ) {
            desfazer();
        }
        if ( btnLimpar.isMousePressed() && !arvore.isVazia() ) {
            dlgLimpar.show();
        }
        if ( btnInicio.isMousePressed() ) {
            irPara( 0 );
        }
        if ( btnVoltar.isMousePressed() ) {
            irPara( indice - 1 );
        }
        if ( btnPlay.isMousePressed() ) {
            alternarPlay();
        }
        if ( btnAvancar.isMousePressed() ) {
            avancar();
        }
        if ( btnFim.isMousePressed() ) {
            irPara( passos.size() - 1 );
        }
        if ( btnCentralizar.isMousePressed() ) {
            centralizar();
        }
        if ( btnAjuda.isMousePressed() ) {
            ajudaVisivel = true;
        }

        velocidade = Math.pow( 2, sldVelocidade.getValue() );

    }

    private void tratarTeclado() {

        if ( isMouseButtonPressed( MOUSE_BUTTON_LEFT ) ) {
            campoFocado = contem( txtChaves, getMouseX(), getMouseY() );
        }
        if ( isKeyPressed( KEY_TAB ) ) {
            campoFocado = false;
        }

        if ( isKeyPressed( KEY_ENTER ) ) {
            operarCampo( TipoOperacao.INSERIR );
        }

        if ( !campoFocado ) {
            if ( isKeyPressed( KEY_SPACE ) ) {
                alternarPlay();
            }
            if ( isKeyPressed( KEY_RIGHT ) ) {
                avancar();
            }
            if ( isKeyPressed( KEY_LEFT ) ) {
                irPara( indice - 1 );
            }
            if ( isKeyPressed( KEY_HOME ) ) {
                irPara( 0 );
            }
            if ( isKeyPressed( KEY_END ) ) {
                irPara( passos.size() - 1 );
            }
        }

    }

    private void tratarMouse() {

        double mx = getMouseX();
        double my = getMouseY();
        boolean naArvore = mx > LARGURA_PAINEL;

        // nó sob o mouse
        idSobMouse = -1;
        Map<Integer, double[]> posicoes = posicoesTela;
        if ( naArvore && !arrastando ) {
            for ( Map.Entry<Integer, double[]> e : posicoes.entrySet() ) {
                double[] p = e.getValue();
                double dx = mx - p[0];
                double dy = my - p[1];
                if ( dx * dx + dy * dy <= p[2] * p[2] ) {
                    idSobMouse = e.getKey();
                }
            }
        }

        // zoom com a roda, mantendo o ponto sob o mouse no lugar
        double roda = getMouseWheelMove();
        if ( naArvore && roda != 0 ) {
            double novoZoom = Math.max( 0.3, Math.min( 4, zoom * Math.pow( 1.15, -roda ) ) );
            double f = novoZoom / zoom;
            double[] area = areaArvore();
            double cx = ( area[0] + area[2] ) / 2;
            double cy = ( area[1] + area[3] ) / 2;
            panX = mx - cx - ( mx - cx - panX ) * f;
            panY = my - cy - ( my - cy - panY ) * f;
            zoom = novoZoom;
        }

        if ( isMouseButtonPressed( MOUSE_BUTTON_LEFT ) && naArvore ) {
            if ( idSobMouse != -1 ) {
                NoFoto no = indexar( atual.raiz ).get( idSobMouse );
                if ( no != null ) {
                    txtChaves.setValue( String.valueOf( no.chave ) );
                    mostrarToast( "Chave " + no.chave + " selecionada: clique em Remover ou Buscar.", corTema );
                }
            } else {
                arrastando = true;
                arrasteX = mx;
                arrasteY = my;
            }
        }

        if ( arrastando ) {
            if ( isMouseButtonDown( MOUSE_BUTTON_LEFT ) ) {
                panX += mx - arrasteX;
                panY += my - arrasteY;
                arrasteX = mx;
                arrasteY = my;
            } else {
                arrastando = false;
            }
        }

    }

    private boolean contem( GuiComponent c, double x, double y ) {
        return x >= c.getX() && x <= c.getX() + c.getWidth()
            && y >= c.getY() && y <= c.getY() + c.getHeight();
    }

    private void atualizarEstadoInterface() {

        boolean fimOperacao = indice >= passos.size() - 1;
        boolean podeRepetir = fimOperacao && fila.isEmpty() && passos.size() > 1;

        if ( podeRepetir ) {
            btnPlay.setText( "Repetir" );
        } else if ( tocando ) {
            btnPlay.setText( "Pausar" );
        } else {
            btnPlay.setText( "Tocar" );
        }

        btnInicio.setEnabled( indice > 0 );
        btnVoltar.setEnabled( indice > 0 );
        btnAvancar.setEnabled( !fimOperacao || !fila.isEmpty() );
        btnFim.setEnabled( !fimOperacao );
        btnDesfazer.setEnabled( !historico.isEmpty() );
        btnLimpar.setEnabled( !arvore.isVazia() );

        barraProgresso.setMax( Math.max( 1, passos.size() - 1 ) );
        barraProgresso.setValue( passos.size() <= 1 ? 1 : indice );

    }

    // ------------------------------------------------------------------
    // operações
    // ------------------------------------------------------------------

    private void operarCampo( TipoOperacao tipo ) {

        String texto = txtChaves.getValue().trim();
        if ( texto.isEmpty() ) {
            mostrarToast( "Digite uma chave (ex.: 42) ou várias separadas por espaço (ex.: 50 30 70).",
                          CORES_PASSO.get( TipoPasso.AVISO ) );
            return;
        }

        String[] partes = texto.split( "[\\s,;]+" );
        List<Integer> chaves = new ArrayList<>();
        for ( String p : partes ) {
            if ( p.isEmpty() ) {
                continue;
            }
            try {
                int k = Integer.parseInt( p );
                if ( k < 0 || k > CHAVE_MAXIMA ) {
                    throw new NumberFormatException();
                }
                chaves.add( k );
            } catch ( NumberFormatException exc ) {
                mostrarToast( "\"" + p + "\" não é válido: use números inteiros de 0 a " + CHAVE_MAXIMA + ".",
                              CORES_PASSO.get( TipoPasso.ERRO ) );
                return;
            }
        }

        for ( int k : chaves ) {
            fila.add( new Operacao( tipo, k ) );
        }
        txtChaves.setValue( "" );

        if ( !chkPausarOperacao.isSelected() ) {
            tocando = true;
        }
        if ( chaves.size() > 1 ) {
            mostrarToast( chaves.size() + " operações na fila.", corTema );
        }

    }

    private void inserirAleatoria() {
        if ( arvore.getTamanho() >= Math.min( MAXIMO_NOS, 99 ) ) {
            mostrarToast( "A árvore já está cheia para chaves aleatórias.", CORES_PASSO.get( TipoPasso.AVISO ) );
            return;
        }
        int k;
        do {
            k = 1 + sorteio.nextInt( 99 );
        } while ( arvore.possuiChave( k ) || naFila( k ) );
        fila.add( new Operacao( TipoOperacao.INSERIR, k ) );
        if ( !chkPausarOperacao.isSelected() ) {
            tocando = true;
        }
    }

    private boolean naFila( int chave ) {
        for ( Operacao op : fila ) {
            if ( op.chave() == chave && op.tipo() == TipoOperacao.INSERIR ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gera uma árvore nova, direto (sem passos).
     */
    private void gerar( boolean crescente ) {

        int n = spnQuantidade.getValue();
        List<Integer> chaves = new ArrayList<>();
        if ( crescente ) {
            for ( int i = 1; i <= n; i++ ) {
                chaves.add( i * 10 );
            }
        } else {
            Set<Integer> usadas = new HashSet<>();
            int limite = n <= 30 ? 99 : 199;
            while ( chaves.size() < n ) {
                int k = 1 + sorteio.nextInt( limite );
                if ( usadas.add( k ) ) {
                    chaves.add( k );
                }
            }
        }

        fila.clear();
        salvarHistorico( crescente ? "gerar crescente" : "gerar aleatória" );
        arvore.limpar();
        arvore.setGravando( false );
        for ( int k : chaves ) {
            arvore.inserir( k );
        }
        arvore.setGravando( true );

        StringBuilder sb = new StringBuilder();
        for ( int k : chaves ) {
            if ( sb.length() > 0 ) {
                sb.append( ' ' );
            }
            sb.append( k );
        }
        mostrarPassoUnico( arvore.fotoAtual( crescente ? "gerar crescente" : "gerar aleatória",
                "Árvore gerada inserindo, nesta ordem: " + sb + "." ) );

    }

    private void carregarExemplo() {
        fila.clear();
        if ( !arvore.isVazia() ) {
            salvarHistorico( "exemplo" );
            arvore.limpar();
            mostrarPassoUnico( arvore.fotoAtual( "exemplo", "Árvore limpa para o exemplo." ) );
        }
        for ( int k : getExemplo() ) {
            fila.add( new Operacao( TipoOperacao.INSERIR, k ) );
        }
        if ( !chkPausarOperacao.isSelected() ) {
            tocando = true;
        }
        espera = TEMPO_PAUSA;
    }

    private void limpar() {
        fila.clear();
        salvarHistorico( "limpar" );
        arvore.limpar();
        mostrarPassoUnico( arvore.fotoAtual( "limpar", "Árvore vazia." ) );
    }

    private void desfazer() {
        fila.clear();
        if ( historico.isEmpty() ) {
            return;
        }
        ArvoreAnimada.Estado estado = historico.pop();
        arvore.restaurarEstado( estado );
        mostrarPassoUnico( arvore.fotoAtual( "desfazer", "Desfeito: " + estado.getDescricao()
                + ". A árvore voltou a ser como era antes." ) );
    }

    private void salvarHistorico( String descricao ) {
        historico.push( arvore.salvarEstado( descricao ) );
        while ( historico.size() > 200 ) {
            historico.removeLast();
        }
    }

    private void mostrarPassoUnico( Passo passo ) {
        passos = new ArrayList<>();
        passos.add( passo );
        mudarPara( 0 );
    }

    /**
     * Tira a próxima operação da fila, executa na árvore e começa a mostrar os passos.
     */
    private void iniciarProxima() {

        Operacao op = fila.poll();
        if ( op == null ) {
            return;
        }

        if ( op.tipo() == TipoOperacao.INSERIR && arvore.getTamanho() >= MAXIMO_NOS
                && !arvore.possuiChave( op.chave() ) ) {
            mostrarToast( "Limite de " + MAXIMO_NOS + " nós atingido.", CORES_PASSO.get( TipoPasso.ERRO ) );
            fila.clear();
            return;
        }

        boolean animar = chkAnimar.isSelected();
        if ( op.tipo() != TipoOperacao.BUSCAR ) {
            salvarHistorico( op.descricao() );
        }

        arvore.setGravando( animar );
        List<Passo> novos;
        switch ( op.tipo() ) {
            case INSERIR:
                novos = arvore.inserir( op.chave() );
                break;
            case REMOVER:
                novos = arvore.remover( op.chave() );
                break;
            default:
                novos = arvore.buscar( op.chave() );
                break;
        }
        arvore.setGravando( true );

        passos = novos;
        mudarPara( 0 );

        if ( animar && chkPausarOperacao.isSelected() ) {
            tocando = false;
        }

    }

    // ------------------------------------------------------------------
    // reprodução dos passos
    // ------------------------------------------------------------------

    private void mudarPara( int i ) {
        // se a transição atual já passou da metade, ela vira o ponto de partida
        if ( progresso >= 0.5 ) {
            anterior = atual;
        }
        indice = Math.max( 0, Math.min( passos.size() - 1, i ) );
        atual = passos.get( indice );
        progresso = 0;
        espera = 0;
    }

    private void irPara( int i ) {
        int alvo = Math.max( 0, Math.min( passos.size() - 1, i ) );
        if ( alvo != indice ) {
            mudarPara( alvo );
        }
    }

    private void avancar() {
        if ( indice < passos.size() - 1 ) {
            mudarPara( indice + 1 );
        } else if ( !fila.isEmpty() ) {
            iniciarProxima();
        }
    }

    private void alternarPlay() {
        boolean fimOperacao = indice >= passos.size() - 1;
        if ( fimOperacao && fila.isEmpty() && passos.size() > 1 ) {
            // repetir a última operação desde o começo
            mudarPara( 0 );
            tocando = true;
        } else {
            tocando = !tocando;
        }
    }

    private double tempoPausa() {
        return TEMPO_PAUSA + atual.texto.length() * 0.01;
    }

    private void atualizarAnimacao( double delta ) {

        // animação desligada: aplica tudo o que está na fila de uma vez
        if ( !chkAnimar.isSelected() ) {
            while ( !fila.isEmpty() ) {
                iniciarProxima();
            }
        }

        if ( progresso < 1 ) {
            progresso = Math.min( 1, progresso + delta * velocidade / TEMPO_TRANSICAO );
            return;
        }

        espera += delta * velocidade;
        if ( !tocando ) {
            return;
        }

        boolean fimOperacao = indice >= passos.size() - 1;
        if ( !fimOperacao ) {
            if ( espera >= tempoPausa() ) {
                mudarPara( indice + 1 );
            }
        } else if ( !fila.isEmpty() ) {
            if ( espera >= tempoPausa() * 0.8 ) {
                iniciarProxima();
            }
        }

    }

    private void mostrarToast( String texto, Color cor ) {
        toast = texto;
        corToast = cor;
        tempoToast = 3.5;
    }

    // ------------------------------------------------------------------
    // layout e câmera
    // ------------------------------------------------------------------

    /**
     * Área da tela reservada para a árvore: {x0, y0, x1, y1}.
     */
    private double[] areaArvore() {
        return new double[]{ LARGURA_PAINEL + 30, 100, getScreenWidth() - 30, getScreenHeight() - 150 };
    }

    /**
     * Posição de cada nó "no mundo": x = posição em ordem, y = nível.
     * Assim nenhum nó fica em cima de outro, qualquer que seja o formato da árvore.
     */
    private Map<Integer, double[]> calcularLayout( NoFoto raiz ) {
        Map<Integer, double[]> mapa = new HashMap<>();
        int[] contador = { 0 };
        posicionar( raiz, 0, contador, mapa );
        return mapa;
    }

    private void posicionar( NoFoto no, int nivel, int[] contador, Map<Integer, double[]> mapa ) {
        if ( no == null ) {
            return;
        }
        posicionar( no.esquerda, nivelFilho( no, no.esquerda, nivel ), contador, mapa );
        mapa.put( no.id, new double[]{ contador[0] * espacoX, nivel * ESPACO_Y } );
        contador[0]++;
        posicionar( no.direita, nivelFilho( no, no.direita, nivel ), contador, mapa );
    }

    private int nivelFilho( NoFoto pai, NoFoto filho, int nivel ) {
        if ( filho != null && isMesmoNivel( pai, filho ) ) {
            return nivel;
        }
        return nivel + 1;
    }

    private double[] calcularEnquadramento( NoFoto raiz ) {
        Map<Integer, double[]> layout = calcularLayout( raiz );
        if ( layout.isEmpty() ) {
            return new double[]{ 0, 0, 1 };
        }
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for ( double[] p : layout.values() ) {
            minX = Math.min( minX, p[0] );
            maxX = Math.max( maxX, p[0] );
            minY = Math.min( minY, p[1] );
            maxY = Math.max( maxY, p[1] );
        }
        double[] area = areaArvore();
        double larguraArea = area[2] - area[0];
        double alturaArea = area[3] - area[1];
        double margem = RAIO * 1.8;
        double larguraConteudo = maxX - minX + 2 * margem;
        double alturaConteudo = maxY - minY + 2 * margem;
        double escala = Math.min( ESCALA_MAXIMA, Math.min( larguraArea / larguraConteudo, alturaArea / alturaConteudo ) );
        return new double[]{ ( minX + maxX ) / 2, ( minY + maxY ) / 2, escala };
    }

    /**
     * Espaço horizontal ideal entre posições em ordem.
     *
     * Dois nós vizinhos "em ordem" nunca estão no mesmo nível, então dá para
     * aproximá-los na horizontal sem sobrepor. Quando a árvore é larga, o
     * espaço diminui até a largura deixar de ser o limite (até um mínimo).
     */
    private double calcularEspacoXAlvo( NoFoto raiz ) {
        int n = NoFoto.tamanho( raiz );
        if ( n <= 1 ) {
            return ESPACO_X_MAXIMO;
        }
        double minimo = getRotuloNo( raiz ) != null ? ESPACO_X_COM_ROTULO : ESPACO_X_MINIMO;
        if ( temMesmoNivel( raiz ) ) {
            minimo = ESPACO_X_MAXIMO;   // na visão 2-3 os pares ficam lado a lado
        }
        int niveis = 0;
        for ( double[] p : calcularLayout( raiz ).values() ) {
            niveis = Math.max( niveis, (int) Math.round( p[1] / ESPACO_Y ) );
        }
        double[] area = areaArvore();
        double margem = RAIO * 1.8;
        double escalaAltura = ( area[3] - area[1] ) / ( niveis * ESPACO_Y + 2 * margem );
        double escalaDesejada = Math.min( ESCALA_MAXIMA, escalaAltura );
        double ideal = ( ( area[2] - area[0] ) / escalaDesejada - 2 * margem ) / ( n - 1 );
        return Math.max( minimo, Math.min( ESPACO_X_MAXIMO, ideal ) );
    }

    private boolean temMesmoNivel( NoFoto no ) {
        if ( no == null ) {
            return false;
        }
        if ( ( no.esquerda != null && isMesmoNivel( no, no.esquerda ) )
          || ( no.direita != null && isMesmoNivel( no, no.direita ) ) ) {
            return true;
        }
        return temMesmoNivel( no.esquerda ) || temMesmoNivel( no.direita );
    }

    private void atualizarCamera( double delta ) {
        double espacoAlvo = calcularEspacoXAlvo( atual.raiz );
        double k = 1 - Math.exp( -delta * 5 );
        espacoX = cameraIniciada ? espacoX + ( espacoAlvo - espacoX ) * k : espacoAlvo;
        double[] alvo = calcularEnquadramento( atual.raiz );
        if ( !cameraIniciada ) {
            camX = alvo[0];
            camY = alvo[1];
            camEscala = alvo[2];
            cameraIniciada = true;
            return;
        }
        camX += ( alvo[0] - camX ) * k;
        camY += ( alvo[1] - camY ) * k;
        camEscala += ( alvo[2] - camEscala ) * k;
    }

    private void centralizar() {
        zoom = 1;
        panX = 0;
        panY = 0;
    }

    private double escala() {
        return camEscala * zoom;
    }

    private double telaX( double mundoX ) {
        double[] area = areaArvore();
        return ( area[0] + area[2] ) / 2 + ( mundoX - camX ) * escala() + panX;
    }

    private double telaY( double mundoY ) {
        double[] area = areaArvore();
        return ( area[1] + area[3] ) / 2 + ( mundoY - camY ) * escala() + panY;
    }

    private static Map<Integer, NoFoto> indexar( NoFoto raiz ) {
        Map<Integer, NoFoto> mapa = new HashMap<>();
        indexar( raiz, mapa );
        return mapa;
    }

    private static void indexar( NoFoto no, Map<Integer, NoFoto> mapa ) {
        if ( no != null ) {
            mapa.put( no.id, no );
            indexar( no.esquerda, mapa );
            indexar( no.direita, mapa );
        }
    }

    private static void mapearPais( NoFoto no, Map<Integer, Integer> pais ) {
        if ( no != null ) {
            if ( no.esquerda != null ) {
                pais.put( no.esquerda.id, no.id );
            }
            if ( no.direita != null ) {
                pais.put( no.direita.id, no.id );
            }
            mapearPais( no.esquerda, pais );
            mapearPais( no.direita, pais );
        }
    }

    private static double suavizar( double t ) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow( -2 * t + 2, 3 ) / 2;
    }

    private static double crescer( double t ) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow( t - 1, 3 ) + c1 * Math.pow( t - 1, 2 );
    }

    private static double lerp( double a, double b, double t ) {
        return a + ( b - a ) * t;
    }

    // ------------------------------------------------------------------
    // desenho
    // ------------------------------------------------------------------

    @Override
    public void draw() {

        // fundo só da área da árvore: o painel lateral (opaco) cobre o resto
        fillRectangle( LARGURA_PAINEL, 0, getScreenWidth() - LARGURA_PAINEL, getScreenHeight(), COR_FUNDO );

        desenharGrade();
        desenharArvore();
        desenharCabecalhoArvore();
        desenharPercurso();
        desenharDescricao();
        desenharToast();

        desenharPainelLateral();

        fonteInterface();
        for ( GuiGroup g : grupos ) {
            g.draw();
        }
        for ( GuiComponent c : componentes ) {
            c.draw();
        }
        desenharTextosPainel();

        desenharInfoNoSobMouse();

        fonteInterface();
        for ( GuiToolTip d : dicas ) {
            d.draw();
        }
        dlgLimpar.draw();
        desenharAjuda();

    }

    // ---- texto ----

    private void fonteInterface() {
        setFontName( Font.MONOSPACED );
        setFontStyle( Font.BOLD );
    }

    protected void texto( String t, double x, double y, int tamanho, int estilo, Color cor ) {
        setFontName( Font.SANS_SERIF );
        setFontStyle( estilo );
        drawText( t, x, y, tamanho, cor );
    }

    protected int larguraTexto( String t, int tamanho, int estilo ) {
        setFontName( Font.SANS_SERIF );
        setFontStyle( estilo );
        return measureText( t, tamanho );
    }

    /**
     * Texto centralizado no ponto (cx, cy).
     */
    protected void textoCentro( String t, double cx, double cy, int tamanho, int estilo, Color cor ) {
        int largura = larguraTexto( t, tamanho, estilo );
        texto( t, cx - largura / 2.0, cy - tamanho * 0.22, tamanho, estilo, cor );
    }

    private List<String> quebrarLinhas( String texto, double largura, int tamanho, int estilo ) {
        List<String> linhas = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for ( String palavra : texto.split( " " ) ) {
            String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
            if ( linha.length() > 0 && larguraTexto( teste, tamanho, estilo ) > largura ) {
                linhas.add( linha.toString() );
                linha = new StringBuilder( palavra );
            } else {
                linha = new StringBuilder( teste );
            }
        }
        if ( linha.length() > 0 ) {
            linhas.add( linha.toString() );
        }
        return linhas;
    }

    /**
     * "Pílula" com texto; devolve a largura usada.
     */
    private double pilula( double x, double y, String t, int tamanho, Color fundo, Color borda, Color cor, Color bolinha ) {
        int lt = larguraTexto( t, tamanho, Font.PLAIN );
        double extra = bolinha != null ? 16 : 0;
        double w = lt + 20 + extra;
        double h = tamanho + 14;
        ChaveTexto chave = new ChaveTexto( t, tamanho, cor.getRGB(), fundo.getRGB(),
                                           borda == null ? 0 : borda.getRGB(),
                                           bolinha == null ? 0 : bolinha.getRGB() );
        Image imagem = cachePilulas.obter( chave, (int) Math.ceil( w ) + 2, (int) Math.ceil( h ) + 2, g -> {
            RoundRectangle2D forma = new RoundRectangle2D.Double( 1, 1, w, h, h, h );
            g.setColor( fundo );
            g.fill( forma );
            if ( borda != null ) {
                g.setStroke( new BasicStroke( 1 ) );
                g.setColor( borda );
                g.draw( forma );
            }
            if ( bolinha != null ) {
                g.setColor( bolinha );
                g.fill( circulo( 1 + 15, 1 + h / 2, 5 ) );
            }
            textoG( g, t, 1 + 10 + extra, 1 + h / 2 - tamanho * 0.22, fonte( Font.PLAIN, tamanho ), cor );
        });
        drawImage( imagem, Math.round( x ) - 1, Math.round( y ) - 1 );
        return w;
    }

    // ---- desenho dentro de imagens (cache) ----

    private static Font fonte( int estilo, int tamanho ) {
        return new Font( Font.SANS_SERIF, estilo, tamanho );
    }

    private static Ellipse2D circulo( double cx, double cy, double r ) {
        return new Ellipse2D.Double( cx - r, cy - r, 2 * r, 2 * r );
    }

    /**
     * Texto numa imagem, posicionado igual ao drawText da JSGE
     * (y é o topo; a linha de base fica meia altura abaixo).
     */
    private static void textoG( Graphics2D g, String t, double x, double y, Font f, Color cor ) {
        g.setFont( f );
        g.setColor( cor );
        double altura = g.getFontMetrics().getStringBounds( t, g ).getHeight();
        g.drawString( t, (int) x, (int) ( y + altura / 2 ) );
    }

    private static void textoCentroG( Graphics2D g, String t, double cx, double cy, Font f, Color cor ) {
        g.setFont( f );
        double x = cx - g.getFontMetrics().stringWidth( t ) / 2.0;
        textoG( g, t, x, cy - f.getSize() * 0.22, f, cor );
    }

    private static List<String> quebrarLinhasG( Graphics2D g, String texto, double largura, Font f ) {
        FontMetrics fm = g.getFontMetrics( f );
        List<String> linhas = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for ( String palavra : texto.split( " " ) ) {
            String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
            if ( linha.length() > 0 && fm.stringWidth( teste ) > largura ) {
                linhas.add( linha.toString() );
                linha = new StringBuilder( palavra );
            } else {
                linha = new StringBuilder( teste );
            }
        }
        if ( linha.length() > 0 ) {
            linhas.add( linha.toString() );
        }
        return linhas;
    }

    /**
     * Corpo do nó (sombra, cor, brilho e borda), desenhado uma vez por cor e raio.
     */
    private Image imagemNo( Color cor, double r ) {
        int raio4 = (int) Math.round( r * 4 );
        double rq = raio4 / 4.0;
        int tamanho = 2 * ( (int) Math.ceil( rq * 1.2 ) + 3 );
        return cacheNos.obter( new ChaveNo( cor.getRGB(), raio4 ), tamanho, tamanho, g -> {
            double c = tamanho / 2.0;
            g.setColor( ColorUtils.fade( Color.BLACK, 0.16 ) );
            g.fill( circulo( c + rq * 0.06, c + rq * 0.14, rq ) );
            g.setColor( cor );
            g.fill( circulo( c, c, rq ) );
            g.setColor( ColorUtils.fade( Color.WHITE, 0.14 ) );
            g.fill( circulo( c - rq * 0.28, c - rq * 0.32, rq * 0.42 ) );
            g.setStroke( new BasicStroke( (float) Math.max( 1.2, rq * 0.09 ) ) );
            g.setColor( ColorUtils.colorBrightness( cor, -0.3 ) );
            g.draw( circulo( c, c, rq ) );
        });
    }

    /**
     * Halo de destaque, desenhado uma vez por tipo de destaque e raio.
     */
    private Image imagemHalo( Destaque destaque, double r ) {
        int raio4 = (int) Math.round( r * 4 );
        double rq = raio4 / 4.0;
        int tamanho = 2 * ( (int) Math.ceil( rq * 1.6 ) + 3 );
        return cacheHalos.obter( new ChaveHalo( destaque, raio4 ), tamanho, tamanho, g -> {
            double c = tamanho / 2.0;
            Color cd = CORES_DESTAQUE.get( destaque );
            if ( destaque == Destaque.CAMINHO ) {
                g.setStroke( new BasicStroke( (float) Math.max( 1.5, rq * 0.12 ) ) );
                g.setColor( ColorUtils.fade( cd, 0.8 ) );
                g.draw( circulo( c, c, rq * 1.22 ) );
            } else {
                g.setColor( ColorUtils.fade( cd, 0.22 ) );
                g.fill( circulo( c, c, rq * 1.6 ) );
                g.setStroke( new BasicStroke( (float) Math.max( 2, rq * 0.17 ) ) );
                g.setColor( cd );
                g.draw( circulo( c, c, rq * 1.25 ) );
            }
        });
    }

    /**
     * Rótulo embaixo do nó (altura, FB...), desenhado uma vez por texto.
     */
    private Image imagemRotulo( String rotulo, int tamanhoFonte, Color cor ) {
        int largura = larguraTexto( rotulo, tamanhoFonte, Font.BOLD ) + 10;
        int altura = tamanhoFonte + 6;
        return cacheRotulos.obter( new ChaveTexto( rotulo, tamanhoFonte, cor.getRGB(), 0, 0, 0 ),
                                   largura + 2, altura + 2, g -> {
            RoundRectangle2D forma = new RoundRectangle2D.Double( 1, 1, largura, altura, altura, altura );
            g.setColor( ColorUtils.fade( Color.WHITE, 0.92 ) );
            g.fill( forma );
            g.setStroke( new BasicStroke( 1 ) );
            g.setColor( ColorUtils.fade( cor, 0.6 ) );
            g.draw( forma );
            textoCentroG( g, rotulo, 1 + largura / 2.0, 1 + altura / 2.0, fonte( Font.BOLD, tamanhoFonte ), cor );
        });
    }

    // ---- fundo ----

    /**
     * Grade do fundo. As linhas são retângulos de 1 pixel: com antialiasing,
     * um drawLine da altura da tela custava ~9 µs; o retângulo, ~1 µs.
     */
    private void desenharGrade() {
        int passo = 32;
        int w = getScreenWidth();
        int h = getScreenHeight();
        int offX = (int) ( ( panX % passo + passo ) % passo );
        int offY = (int) ( ( panY % passo + passo ) % passo );
        for ( int x = LARGURA_PAINEL + offX; x < w; x += passo ) {
            fillRectangle( x, 0, 1, h, COR_GRADE );
        }
        for ( int y = offY; y < h; y += passo ) {
            fillRectangle( LARGURA_PAINEL, y, w - LARGURA_PAINEL, 1, COR_GRADE );
        }
    }

    // ---- árvore ----

    private void desenharArvore() {

        // montado inteiro e só publicado no fim: o update() (outra thread) lê este mapa
        Map<Integer, double[]> novasPosicoes = new HashMap<>();

        double e = suavizar( progresso );
        double s = escala();
        double r = RAIO * s;

        Map<Integer, double[]> layoutA = calcularLayout( anterior.raiz );
        Map<Integer, double[]> layoutB = calcularLayout( atual.raiz );
        Map<Integer, NoFoto> nosA = indexar( anterior.raiz );
        Map<Integer, NoFoto> nosB = indexar( atual.raiz );
        Map<Integer, Integer> paisA = new HashMap<>();
        Map<Integer, Integer> paisB = new HashMap<>();
        mapearPais( anterior.raiz, paisA );
        mapearPais( atual.raiz, paisB );

        // posição na tela de cada nó: {x, y, escala, alfa}
        Map<Integer, double[]> pos = new HashMap<>();
        for ( Map.Entry<Integer, double[]> en : layoutB.entrySet() ) {
            int id = en.getKey();
            double[] b = en.getValue();
            double[] a = layoutA.get( id );
            double esc = 1;
            double alfa = 1;
            double[] origem = a;
            if ( origem == null ) {
                // nó novo: nasce na posição do pai e cresce
                Integer pai = paisB.get( id );
                origem = b;
                if ( pai != null ) {
                    origem = layoutA.containsKey( pai ) ? layoutA.get( pai ) : layoutB.get( pai );
                }
                esc = Math.max( 0, crescer( progresso ) );
                alfa = Math.min( 1, progresso * 2.5 );
            }
            double wx = lerp( origem[0], b[0], e );
            double wy = lerp( origem[1], b[1], e );
            pos.put( id, new double[]{ telaX( wx ), telaY( wy ), esc, alfa } );
        }

        // nós removidos: somem no lugar onde estavam
        List<Integer> removidos = new ArrayList<>();
        for ( Map.Entry<Integer, double[]> en : layoutA.entrySet() ) {
            if ( !layoutB.containsKey( en.getKey() ) ) {
                double[] a = en.getValue();
                removidos.add( en.getKey() );
                pos.put( en.getKey(), new double[]{ telaX( a[0] ), telaY( a[1] ), 1 - 0.5 * e, 1 - e } );
            }
        }

        // arestas que vão sumir (ligadas a nós removidos)
        for ( Map.Entry<Integer, Integer> pf : paisA.entrySet() ) {
            int filho = pf.getKey();
            int pai = pf.getValue();
            if ( removidos.contains( filho ) || removidos.contains( pai ) ) {
                double[] p1 = pos.get( pai );
                double[] p2 = pos.get( filho );
                if ( p1 != null && p2 != null ) {
                    NoFoto f = nosA.get( filho );
                    setStrokeLineWidth( (float) ( getLarguraAresta( f ) * s ) );
                    drawLine( p1[0], p1[1], p2[0], p2[1], ColorUtils.fade( getCorAresta( f ), 1 - e ) );
                }
            }
        }

        // arestas da foto atual
        for ( Map.Entry<Integer, Integer> pf : paisB.entrySet() ) {
            int filho = pf.getKey();
            int pai = pf.getValue();
            double[] p1 = pos.get( pai );
            double[] p2 = pos.get( filho );
            NoFoto fB = nosB.get( filho );
            NoFoto fA = nosA.get( filho );
            Color cor = getCorAresta( fB );
            float largura = getLarguraAresta( fB );
            if ( fA != null ) {
                cor = ColorUtils.lerp( getCorAresta( fA ), cor, e );
                largura = (float) lerp( getLarguraAresta( fA ), largura, e );
            }
            double alfa = Math.min( p1[3], p2[3] );
            setStrokeLineWidth( (float) ( largura * s ) );
            drawLine( p1[0], p1[1], p2[0], p2[1], ColorUtils.fade( cor, alfa ) );
        }

        // links nulos
        if ( chkNulos.isSelected() ) {
            for ( NoFoto no : nosB.values() ) {
                double[] p = pos.get( no.id );
                double alfa = Math.min( p[3], e );
                if ( no.esquerda == null ) {
                    desenharNulo( p[0], p[1], -1, s, alfa );
                }
                if ( no.direita == null ) {
                    desenharNulo( p[0], p[1], 1, s, alfa );
                }
            }
        }

        // nós que estão sumindo
        for ( int id : removidos ) {
            double[] p = pos.get( id );
            NoFoto no = nosA.get( id );
            desenharNo( no, no.chave, p[0], p[1], r * p[2], p[3], anterior.getDestaque( id ), 1, getCorNo( no ), true );
        }

        // nós da foto atual
        double tempo = getTime();
        for ( NoFoto no : nosB.values() ) {
            double[] p = pos.get( no.id );
            NoFoto noA = nosA.get( no.id );
            Color cor = getCorNo( no );
            boolean corEstavel = true;
            if ( noA != null && e < 1 && getCorNo( noA ).getRGB() != cor.getRGB() ) {
                cor = ColorUtils.lerp( getCorNo( noA ), cor, e );
                corEstavel = false;   // cor mudando: desenha forma a forma
            }
            int chave = no.chave;
            if ( atual.copiaParaId == no.id && noA != null && progresso < 0.92 ) {
                chave = noA.chave;
            }
            Destaque d = atual.getDestaque( no.id );
            double alfaDestaque = ( d != null && noA != null && anterior.getDestaque( no.id ) == d ) ? 1 : e;
            double raio = r * p[2];
            if ( d == Destaque.VISITANDO ) {
                raio *= 1 + 0.04 * Math.sin( tempo * 9 );
            }
            desenharNo( no, chave, p[0], p[1], raio, p[3], d, alfaDestaque, cor, corEstavel );
            novasPosicoes.put( no.id, new double[]{ p[0], p[1], Math.max( raio, 8 ) } );
        }
        posicoesTela = novasPosicoes;

        // chave "voando" do sucessor para o nó que recebe a cópia
        if ( atual.copiaDeId >= 0 && progresso < 1 ) {
            double[] de = pos.get( atual.copiaDeId );
            double[] para = pos.get( atual.copiaParaId );
            NoFoto destino = nosB.get( atual.copiaParaId );
            if ( de != null && para != null && destino != null ) {
                double x = lerp( de[0], para[0], e );
                double y = lerp( de[1], para[1], e ) - Math.sin( Math.PI * e ) * 50 * s;
                Color c = CORES_DESTAQUE.get( Destaque.SUCESSOR );
                int fs = (int) Math.max( 9, r * 0.85 );
                String t = String.valueOf( destino.chave );
                double lw = larguraTexto( t, fs, Font.BOLD ) + 14;
                fillRoundRectangle( x - lw / 2, y - fs * 0.85, lw, fs * 1.7, fs * 1.7, c );
                textoCentro( t, x, y, fs, Font.BOLD, Color.WHITE );
            }
        }

        if ( nosB.isEmpty() && removidos.isEmpty() ) {
            double[] area = areaArvore();
            textoCentro( "árvore vazia", ( area[0] + area[2] ) / 2, ( area[1] + area[3] ) / 2,
                         22, Font.BOLD, ColorUtils.fade( COR_TEXTO_SUAVE, 0.5 ) );
        }

    }

    private void desenharNulo( double x, double y, int lado, double s, double alfa ) {
        double nx = x + lado * Math.min( espacoX, 40 ) * 0.5 * s;
        double ny = y + ESPACO_Y * 0.5 * s;
        double t = 5 * s;
        setStrokeLineWidth( (float) Math.max( 1, 1.2 * s ) );
        drawLine( x, y, nx, ny - t, ColorUtils.fade( COR_NULO, alfa ) );
        fillRectangle( nx - t, ny - t, 2 * t, 2 * t, ColorUtils.fade( COR_NULO, alfa ) );
    }

    /**
     * Desenha um nó. Parado (opaco e com a cor fixa), o nó vem pronto do
     * cache de imagens; durante uma transição (aparecendo, sumindo ou
     * trocando de cor) é desenhado forma a forma.
     */
    private void desenharNo( NoFoto no, int chave, double x, double y, double r, double alfa,
                             Destaque destaque, double alfaDestaque, Color cor, boolean corEstavel ) {

        if ( r <= 0.5 || alfa <= 0.01 ) {
            return;
        }

        boolean opaco = alfa >= 0.999;

        // halo do destaque
        if ( destaque != null ) {
            Color cd = CORES_DESTAQUE.get( destaque );
            double a = alfa * alfaDestaque;
            if ( a >= 0.999 ) {
                Image halo = imagemHalo( destaque, r );
                int c = halo.getWidth() / 2;
                drawImage( halo, Math.round( x ) - c, Math.round( y ) - c );
            } else if ( destaque == Destaque.CAMINHO ) {
                setStrokeLineWidth( (float) Math.max( 1.5, r * 0.12 ) );
                drawCircle( x, y, r * 1.22, ColorUtils.fade( cd, 0.8 * a ) );
            } else {
                fillCircle( x, y, r * 1.6, ColorUtils.fade( cd, 0.22 * a ) );
                setStrokeLineWidth( (float) Math.max( 2, r * 0.17 ) );
                drawCircle( x, y, r * 1.25, ColorUtils.fade( cd, a ) );
            }
        }

        // sombra, corpo, brilho e borda
        if ( opaco && corEstavel ) {
            Image corpo = imagemNo( cor, r );
            int c = corpo.getWidth() / 2;
            drawImage( corpo, Math.round( x ) - c, Math.round( y ) - c );
        } else {
            fillCircle( x + r * 0.06, y + r * 0.14, r, ColorUtils.fade( Color.BLACK, 0.16 * alfa ) );
            fillCircle( x, y, r, ColorUtils.fade( cor, alfa ) );
            fillCircle( x - r * 0.28, y - r * 0.32, r * 0.42, ColorUtils.fade( Color.WHITE, 0.14 * alfa ) );
            setStrokeLineWidth( (float) Math.max( 1.2, r * 0.09 ) );
            drawCircle( x, y, r, ColorUtils.fade( ColorUtils.colorBrightness( cor, -0.3 ), alfa ) );
        }

        // chave
        String t = String.valueOf( chave );
        double fator = t.length() >= 3 ? 0.72 : 0.9;
        int fs = (int) Math.max( 7, r * fator );
        textoCentro( t, x, y, fs, Font.BOLD, ColorUtils.fade( Color.WHITE, alfa ) );

        // rótulo embaixo (altura, FB...)
        String rotulo = getRotuloNo( no );
        if ( rotulo != null && r > 9 ) {
            int fr = (int) Math.max( 8, r * 0.5 );
            Color cr = getCorRotuloNo( no );
            if ( opaco ) {
                Image imagem = imagemRotulo( rotulo, fr, cr );
                double lw = imagem.getWidth() - 2;
                drawImage( imagem, Math.round( x - lw / 2 ) - 1, Math.round( y + r + 4 ) - 1 );
            } else {
                double lw = larguraTexto( rotulo, fr, Font.BOLD ) + 10;
                double lh = fr + 6;
                double ly = y + r + 4;
                fillRoundRectangle( x - lw / 2, ly, lw, lh, lh, ColorUtils.fade( Color.WHITE, 0.92 * alfa ) );
                setStrokeLineWidth( 1 );
                drawRoundRectangle( x - lw / 2, ly, lw, lh, lh, ColorUtils.fade( cr, 0.6 * alfa ) );
                textoCentro( rotulo, x, ly + lh / 2, fr, Font.BOLD, ColorUtils.fade( cr, alfa ) );
            }
        }

    }

    // ---- informações no topo da área da árvore ----

    private void desenharCabecalhoArvore() {

        double x = LARGURA_PAINEL + 16;
        double y = 14;
        NoFoto raiz = atual.raiz;
        int n = NoFoto.tamanho( raiz );
        int altura = NoFoto.altura( raiz );
        int minima = n == 0 ? 0 : (int) Math.ceil( Math.log( n + 1 ) / Math.log( 2 ) );

        List<String> itens = new ArrayList<>();
        itens.add( n + ( n == 1 ? " nó" : " nós" ) );
        itens.add( "altura " + altura );
        itens.add( "altura mínima possível " + minima );
        adicionarEstatisticas( raiz, itens );

        for ( String item : itens ) {
            x += pilula( x, y, item, 13, COR_PAINEL, COR_BORDA, COR_TEXTO, null ) + 8;
        }
        double fimInfo = x;

        // legenda: destaques usados no passo atual + itens fixos
        List<ItemLegenda> legenda = new ArrayList<>();
        adicionarLegendaFixa( legenda );
        Set<Destaque> usados = new HashSet<>( atual.destaques.values() );
        for ( Destaque d : Destaque.values() ) {
            if ( usados.contains( d ) ) {
                legenda.add( new ItemLegenda( CORES_DESTAQUE.get( d ), d.getDescricao() ) );
            }
        }

        double larguraTotal = 0;
        for ( ItemLegenda it : legenda ) {
            larguraTotal += larguraTexto( it.texto(), 12, Font.PLAIN ) + 36 + 6;
        }
        double lx = getScreenWidth() - 16 - larguraTotal;
        double ly = 14;
        if ( lx < fimInfo + 10 ) {
            lx = LARGURA_PAINEL + 16;
            ly = 50;
        }
        for ( ItemLegenda it : legenda ) {
            lx += pilula( lx, ly + 1, it.texto(), 12, ColorUtils.fade( COR_PAINEL, 0.9 ), COR_BORDA,
                          COR_TEXTO_SUAVE, it.cor() ) + 6;
        }

    }

    private void desenharPercurso() {

        Percurso tipo = Percurso.values()[Math.max( 0, lstPercurso.getSelectedItemIndex() )];
        List<Integer> chaves = NoFoto.percorrer( atual.raiz, tipo );
        StringBuilder sb = new StringBuilder();
        for ( int k : chaves ) {
            if ( sb.length() > 0 ) {
                sb.append( "  " );
            }
            sb.append( k );
        }
        String rotulo = tipo.getNome() + ":";
        String valores = chaves.isEmpty() ? "(vazio)" : sb.toString();

        double x = LARGURA_PAINEL + 16;
        double y = getScreenHeight() - 132;
        double w = getScreenWidth() - LARGURA_PAINEL - 32;
        int largura = (int) Math.ceil( w ) + 2;

        // a barra só é desenhada de novo quando o percurso (ou a largura) muda
        Image imagem = cachePaineis.obter( new ChavePainel( rotulo, valores, corTema.getRGB(), largura, 30 ),
                                           largura, 30, g -> {
            RoundRectangle2D forma = new RoundRectangle2D.Double( 1, 1, w, 28, 12, 12 );
            g.setColor( ColorUtils.fade( COR_PAINEL, 0.85 ) );
            g.fill( forma );
            g.setStroke( new BasicStroke( 1 ) );
            g.setColor( COR_BORDA );
            g.draw( forma );
            Font negrito = fonte( Font.BOLD, 13 );
            Font normal = fonte( Font.PLAIN, 13 );
            FontMetrics fm = g.getFontMetrics( normal );
            int lr = g.getFontMetrics( negrito ).stringWidth( rotulo );
            double maxValores = w - lr - 34;
            String v = valores;
            if ( fm.stringWidth( v ) > maxValores ) {
                while ( v.length() > 3 && fm.stringWidth( v + " ..." ) > maxValores ) {
                    v = v.substring( 0, v.length() - 1 );
                }
                v = v + " ...";
            }
            textoG( g, rotulo, 1 + 12, 1 + 14 - 13 * 0.22, negrito, corTema );
            textoG( g, v, 1 + 18 + lr, 1 + 14 - 13 * 0.22, normal, COR_TEXTO );
        });
        drawImage( imagem, Math.round( x ) - 1, Math.round( y ) - 1 );

    }

    private void desenharDescricao() {

        double x = LARGURA_PAINEL + 16;
        double h = 88;
        double y = getScreenHeight() - h - 14;
        double w = getScreenWidth() - LARGURA_PAINEL - 32;

        Color corTipo = atual.tipo == TipoPasso.CONCLUIDO ? corTema : CORES_PASSO.get( atual.tipo );

        // linha de cima: operação e passo
        StringBuilder topo = new StringBuilder();
        if ( !atual.operacao.isEmpty() ) {
            topo.append( atual.operacao ).append( "   ·   " );
        }
        topo.append( "passo " ).append( indice + 1 ).append( " de " ).append( passos.size() );
        if ( !fila.isEmpty() ) {
            topo.append( "   ·   " ).append( fila.size() ).append( fila.size() == 1 ? " operação" : " operações" )
                .append( " na fila" );
        }
        String textoTopo = topo.toString();
        String texto = atual.texto;
        int largura = (int) Math.ceil( w ) + 4;
        int altura = (int) h + 6;

        // a caixa só é desenhada de novo quando o passo (ou a largura) muda
        Image imagem = cachePaineis.obter( new ChavePainel( textoTopo, texto, corTipo.getRGB(), largura, altura ),
                                           largura, altura, g -> {
            g.setColor( ColorUtils.fade( Color.BLACK, 0.06 ) );
            g.fill( new RoundRectangle2D.Double( 2, 4, w, h, 16, 16 ) );
            RoundRectangle2D forma = new RoundRectangle2D.Double( 1, 1, w, h, 16, 16 );
            g.setColor( COR_PAINEL );
            g.fill( forma );
            g.setStroke( new BasicStroke( 1 ) );
            g.setColor( COR_BORDA );
            g.draw( forma );
            g.setColor( corTipo );
            g.fill( new RoundRectangle2D.Double( 1 + 10, 1 + 12, 6, h - 24, 6, 6 ) );
            textoG( g, textoTopo, 1 + 28, 1 + 10, fonte( Font.BOLD, 13 ), corTipo );
            Font normal = fonte( Font.PLAIN, 16 );
            List<String> linhas = quebrarLinhasG( g, texto, w - 48, normal );
            double ly = 1 + 34;
            for ( int i = 0; i < linhas.size() && i < 3; i++ ) {
                textoG( g, linhas.get( i ), 1 + 28, ly, normal, COR_TEXTO );
                ly += 21;
            }
        });
        drawImage( imagem, Math.round( x ) - 1, Math.round( y ) - 1 );

    }

    /**
     * Aviso temporário: aparece no canto de cima da caixa de explicação.
     */
    private void desenharToast() {
        if ( tempoToast <= 0 || toast == null ) {
            return;
        }
        double alfa = Math.min( 1, tempoToast / 0.4 );
        int fs = 13;
        double lw = larguraTexto( toast, fs, Font.BOLD ) + 28;
        double lh = 26;
        double xDireita = getScreenWidth() - 16 - 12;
        double y = getScreenHeight() - 88 - 14 + 6;
        fillRoundRectangle( xDireita - lw, y, lw, lh, lh, ColorUtils.fade( corToast, 0.95 * alfa ) );
        textoCentro( toast, xDireita - lw / 2, y + lh / 2, fs, Font.BOLD, ColorUtils.fade( Color.WHITE, alfa ) );
    }

    // ---- painel lateral ----

    private void desenharPainelLateral() {

        int h = getScreenHeight();
        fillRectangle( 0, 0, LARGURA_PAINEL, h, COR_PAINEL );
        setStrokeLineWidth( 1 );
        drawLine( LARGURA_PAINEL, 0, LARGURA_PAINEL, h, COR_BORDA );
        fillRectangle( 0, 0, LARGURA_PAINEL, 5, corTema );

        // ícone: uma arvorezinha (desenhada uma vez numa imagem)
        Image icone = cachePaineis.obter( new ChavePainel( "icone", "", corTema.getRGB(), 0, 0 ), 52, 56, g -> {
            g.setStroke( new BasicStroke( 2 ) );
            g.setColor( ColorUtils.colorBrightness( corTema, -0.15 ) );
            g.draw( new java.awt.geom.Line2D.Double( 30, 26, 21, 44 ) );
            g.draw( new java.awt.geom.Line2D.Double( 30, 26, 39, 44 ) );
            g.setColor( corTema );
            g.fill( circulo( 30, 25, 7 ) );
            g.setColor( ColorUtils.colorBrightness( corTema, 0.2 ) );
            g.fill( circulo( 21, 45, 6 ) );
            g.fill( circulo( 39, 45, 6 ) );
        });
        drawImage( icone, 0, 0 );

        // título: diminui a fonte até caber ao lado do botão "?"
        if ( tamanhoTitulo == 0 ) {
            int ft = 19;
            while ( ft > 13 && larguraTexto( getTitulo(), ft, Font.BOLD ) > LARGURA_PAINEL - 52 - 52 ) {
                ft--;
            }
            tamanhoTitulo = ft;
        }
        texto( getTitulo(), 52, 18 + ( 19 - tamanhoTitulo ) * 0.5, tamanhoTitulo, Font.BOLD, COR_TEXTO );
        texto( getSubtitulo(), 52, 45, 12, Font.PLAIN, COR_TEXTO_SUAVE );

        // atalhos no rodapé do painel (só se couberem; também estão na ajuda)
        double yAtalhos = Math.max( yFimPainel + 12, h - 46 );
        if ( yAtalhos + 30 <= h ) {
            texto( "Enter: inserir   ·   Espaço: tocar/pausar", 16, yAtalhos, 11, Font.PLAIN, COR_TEXTO_SUAVE );
            texto( "Setas: passos   ·   Roda: zoom   ·   Arraste: mover", 16, yAtalhos + 17, 11, Font.PLAIN, COR_TEXTO_SUAVE );
        }

    }

    private void desenharTextosPainel() {

        // dica dentro do campo vazio
        if ( txtChaves.getValue().isEmpty() && !campoFocado ) {
            fonteInterface();
            drawText( "ex.: 42  ou  50 30 70", txtChaves.getX() + 6, txtChaves.getY() + 9, 12,
                      ColorUtils.fade( COR_TEXTO_SUAVE, 0.7 ) );
        }

        double xi = 26;
        String rotuloPasso = "Passo " + ( indice + 1 ) + " de " + passos.size();
        if ( !atual.operacao.isEmpty() ) {
            rotuloPasso += "  ·  " + atual.operacao;
        }
        texto( rotuloPasso, xi, yRotuloProgresso, 12, Font.PLAIN, COR_TEXTO_SUAVE );
        if ( velocidade != velocidadeMostrada ) {
            velocidadeMostrada = velocidade;
            textoVelocidade = String.format( "Velocidade %.2fx", velocidade );
        }
        texto( textoVelocidade, xi, yRotuloVelocidade, 12, Font.PLAIN, COR_TEXTO );

    }

    /**
     * Quadro da ajuda: {x, y, largura, altura}.
     */
    private double[] quadroAjuda() {
        double w = Math.min( 720, getScreenWidth() - 80 );
        List<String> linhas = linhasAjuda( w - 56 );
        int titulos = 0;
        for ( String l : linhas ) {
            if ( l.startsWith( "#" ) ) {
                titulos++;
            }
        }
        double h = 70 + linhas.size() * 22 + titulos * 6 + 70;
        return new double[]{ ( getScreenWidth() - w ) / 2, ( getScreenHeight() - h ) / 2, w, h };
    }

    private List<String> linhasAjuda( double largura ) {
        List<String> linhas = new ArrayList<>();
        for ( String paragrafo : textoAjuda().split( "\n" ) ) {
            if ( paragrafo.startsWith( "#" ) ) {
                linhas.add( paragrafo );
            } else {
                linhas.addAll( quebrarLinhas( paragrafo, largura, 14, Font.PLAIN ) );
            }
        }
        return linhas;
    }

    private void desenharAjuda() {

        if ( !ajudaVisivel ) {
            return;
        }

        fillRectangle( 0, 0, getScreenWidth(), getScreenHeight(), ColorUtils.fade( Color.BLACK, 0.35 ) );
        double[] q = quadroAjuda();
        quadroAjudaDesenhado = q;
        fillRoundRectangle( q[0] + 3, q[1] + 5, q[2], q[3], 20, ColorUtils.fade( Color.BLACK, 0.15 ) );
        fillRoundRectangle( q[0], q[1], q[2], q[3], 20, COR_PAINEL );
        fillRoundRectangle( q[0], q[1], q[2], 8, 8, corTema );

        texto( "Como usar  ·  " + getTitulo(), q[0] + 28, q[1] + 26, 20, Font.BOLD, COR_TEXTO );

        double y = q[1] + 70;
        boolean primeiro = true;
        for ( String linha : linhasAjuda( q[2] - 56 ) ) {
            if ( linha.startsWith( "#" ) ) {
                if ( !primeiro ) {
                    y += 6;
                }
                texto( linha.substring( 1 ).trim().toUpperCase(), q[0] + 28, y, 13, Font.BOLD, corTema );
            } else {
                texto( linha, q[0] + 28, y, 14, Font.PLAIN, COR_TEXTO );
            }
            y += 22;
            primeiro = false;
        }

        double bx = q[0] + q[2] - 28 - btnFecharAjuda.getWidth();
        double by = q[1] + q[3] - 20 - btnFecharAjuda.getHeight();
        btnFecharAjuda.move( bx - btnFecharAjuda.getX(), by - btnFecharAjuda.getY() );
        fonteInterface();
        btnFecharAjuda.draw();
        texto( "Esc ou clique fora para fechar", q[0] + 28, by + 9, 12, Font.PLAIN, COR_TEXTO_SUAVE );

    }

    private void desenharInfoNoSobMouse() {

        if ( idSobMouse == -1 || arrastando ) {
            return;
        }
        NoFoto no = indexar( atual.raiz ).get( idSobMouse );
        double[] p = posicoesTela.get( idSobMouse );
        if ( no == null || p == null ) {
            return;
        }

        List<String> linhas = new ArrayList<>();
        linhas.add( "Chave " + no.chave );
        linhas.add( "Altura: " + no.altura + "   ·   Subárvore: " + no.tamanho + ( no.tamanho == 1 ? " nó" : " nós" ) );
        adicionarInfoNo( no, linhas );
        linhas.add( "Clique para usar esta chave" );

        double w = 0;
        for ( String l : linhas ) {
            w = Math.max( w, larguraTexto( l, 13, Font.PLAIN ) );
        }
        w += 24;
        double h = linhas.size() * 19 + 14;
        double x = p[0] + p[2] + 12;
        double y = p[1] - h / 2;
        if ( x + w > getScreenWidth() - 8 ) {
            x = p[0] - p[2] - 12 - w;
        }
        y = Math.max( 8, Math.min( getScreenHeight() - h - 8, y ) );

        fillRoundRectangle( x + 2, y + 3, w, h, 12, ColorUtils.fade( Color.BLACK, 0.12 ) );
        fillRoundRectangle( x, y, w, h, 12, new Color( 30, 41, 59 ) );
        double ly = y + 9;
        for ( int i = 0; i < linhas.size(); i++ ) {
            boolean ultima = i == linhas.size() - 1;
            Color c = i == 0 ? Color.WHITE : ultima ? new Color( 148, 163, 184 ) : new Color( 226, 232, 240 );
            texto( linhas.get( i ), x + 12, ly, 13, i == 0 ? Font.BOLD : Font.PLAIN, c );
            ly += 19;
        }

    }

}
