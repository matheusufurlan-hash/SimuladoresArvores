package arvores.visual;

import br.com.davidbuzatto.jsge.image.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Guarda imagens já desenhadas para não desenhar a mesma coisa a cada quadro.
 *
 * Por que existe: formas com antialiasing (círculos, retângulos arredondados)
 * são caras para o Java desenhar. Um nó, por exemplo, custava ~140 µs a cada
 * quadro. Desenhando o nó uma única vez numa imagem pequena e depois só
 * copiando essa imagem, o custo cai para alguns µs.
 *
 * A chave descreve tudo o que muda o desenho (cor, raio, texto...). Se algo
 * mudar, a chave é outra e a imagem é desenhada de novo: nunca aparece uma
 * imagem velha na tela.
 *
 * @author Matheus Ubinha Furlan
 */
final class CacheImagens {

    private final Map<Object, Image> imagens;
    private final int limite;

    CacheImagens( int limite ) {
        this.imagens = new HashMap<>();
        this.limite = limite;
    }

    /**
     * Devolve a imagem da chave; se ainda não existe, cria com o tamanho
     * pedido (fundo transparente) e chama "desenho" para desenhar nela.
     */
    Image obter( Object chave, int largura, int altura, Consumer<Graphics2D> desenho ) {
        Image imagem = imagens.get( chave );
        if ( imagem == null ) {
            if ( imagens.size() >= limite ) {
                imagens.clear();   // simples: quando enche, começa de novo
            }
            imagem = new Image( Math.max( 1, largura ), Math.max( 1, altura ) );
            Graphics2D g = imagem.buffImage.createGraphics();
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            desenho.accept( g );
            g.dispose();
            imagens.put( chave, imagem );
        }
        return imagem;
    }

}
