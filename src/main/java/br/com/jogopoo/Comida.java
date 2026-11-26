package br.com.jogopoo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.BasicStroke;

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
        int x = getX() * tamanhoCelula + margem;
        int y = getY() * tamanhoCelula + margem;
        int diametro = tamanhoCelula - margem * 2;

        graficos.setColor(new Color(248, 113, 113, 35));
        graficos.fillOval(
                x - 4,
                y - 4,
                diametro + 8,
                diametro + 8);
        graficos.setColor(new Color(5, 10, 20, 75));
        graficos.fillOval(x + 2, y + 3, diametro, diametro);

        GradientPaint fruta = new GradientPaint(
                x, y, new Color(255, 190, 120),
                x + diametro, y + diametro, COR);
        graficos.setPaint(fruta);
        graficos.fillOval(
                x,
                y,
                diametro,
                diametro);
        graficos.setColor(new Color(255, 255, 255, 150));
        graficos.fillOval(x + diametro / 4, y + diametro / 5, Math.max(2, diametro / 5), Math.max(2, diametro / 5));

        graficos.setColor(new Color(74, 222, 128));
        graficos.setStroke(new BasicStroke(2f));
        graficos.drawLine(
                x + diametro / 2,
                y + 2,
                x + diametro / 2 + 3,
                y - 2);
        graficos.fillOval(x + diametro / 2 + 2, y - 3, 5, 3);
    }
}
