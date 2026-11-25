package br.com.jogopoo;

import java.awt.Graphics2D;

public abstract class ElementoJogo {
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

    public abstract void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso);
}
