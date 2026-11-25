package br.com.jogopoo;

import java.awt.Color;
import java.awt.Graphics2D;

public class Comida extends ElementoJogo {
    private static final Color COR = new Color(248, 113, 113);

    public Comida(int x, int y) {
        super(x, y);
    }

    void reposicionar(int x, int y) {
        definirPosicao(x, y);
    }

    @Override
    public void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso) {
        int margem = tamanhoCelula / 5;
        graficos.setColor(COR);
        graficos.fillOval(
                getX() * tamanhoCelula + margem,
                getY() * tamanhoCelula + margem,
                tamanhoCelula - margem * 2,
                tamanhoCelula - margem * 2);
    }
}
