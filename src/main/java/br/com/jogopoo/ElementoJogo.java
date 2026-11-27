package br.com.jogopoo;

import java.awt.Graphics2D;

/**
 * Abstração comum dos objetos posicionados no tabuleiro.
 *
 * <p>Encapsula as coordenadas e define o contrato de desenho implementado
 * polimorficamente pelas subclasses.</p>
 */
public abstract class ElementoJogo {
    // A posição é alterada apenas pelas operações controladas desta hierarquia.
    private int x;
    private int y;

    protected ElementoJogo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    protected final void definirPosicao(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Desenha o elemento, permitindo que cada subtipo forneça sua própria representação. */
    public abstract void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso);
}
