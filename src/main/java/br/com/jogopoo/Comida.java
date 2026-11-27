package br.com.jogopoo;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Especializa {@link ElementoJogo} para representar a comida coletável.
 *
 * <p>Herda e encapsula sua posição na classe base e implementa seu próprio desenho.</p>
 */
public class Comida extends ElementoJogo {
    private static final int CHANCE_FRUTA_ESPECIAL = 10;
    private static final TipoFruta[] FRUTAS_COMUNS = {
        TipoFruta.MACA,
        TipoFruta.LARANJA,
        TipoFruta.LIMAO,
        TipoFruta.UVA,
        TipoFruta.BANANA,
        TipoFruta.CEREJA,
        TipoFruta.KIWI
    };

    private TipoFruta tipo = TipoFruta.MACA;

    public Comida(int x, int y) {
        super(x, y);
    }

    void reposicionar(int x, int y) {
        definirPosicao(x, y);
        ThreadLocalRandom aleatorio = ThreadLocalRandom.current();
        tipo = aleatorio.nextInt(100) < CHANCE_FRUTA_ESPECIAL
                ? TipoFruta.ESPECIAL
                : FRUTAS_COMUNS[aleatorio.nextInt(FRUTAS_COMUNS.length)];
    }

    public TipoFruta getTipo() {
        return tipo;
    }

    @Override
    public void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso) {
        // Compõe a fruta selecionada com brilho, sombra e detalhes dentro da célula.
        int margem = tamanhoCelula / 5;
        int x = getX() * tamanhoCelula + margem;
        int y = getY() * tamanhoCelula + margem;
        int diametro = tamanhoCelula - margem * 2;

        graficos.setColor(tipo.aura);
        graficos.fillOval(
                x - 4,
                y - 4,
                diametro + 8,
                diametro + 8);
        graficos.setColor(new Color(5, 10, 20, 75));
        graficos.fillOval(x + 2, y + 3, diametro, diametro);

        GradientPaint fruta = new GradientPaint(
                x, y, tipo.corClara,
                x + diametro, y + diametro, tipo.corEscura);
        graficos.setPaint(fruta);
        switch (tipo) {
            case MACA:
                graficos.fillOval(x + 1, y + 1, diametro - 2, diametro - 1);
                graficos.fillOval(x + 2, y, diametro / 2, diametro / 2);
                graficos.fillOval(x + diametro / 2, y, diametro / 2, diametro / 2);
                break;
            case LARANJA:
                graficos.fillOval(x, y, diametro, diametro);
                graficos.setColor(new Color(255, 231, 173, 180));
                graficos.fillOval(x + diametro / 2, y + diametro / 2, 2, 2);
                break;
            case LIMAO:
                graficos.fillOval(x - 1, y + 2, diametro + 2, diametro - 4);
                break;
            case UVA:
                int tamanhoBaga = Math.max(4, diametro / 2);
                int centro = x + (diametro - tamanhoBaga) / 2;
                graficos.fillOval(centro, y, tamanhoBaga, tamanhoBaga);
                graficos.fillOval(x, y + diametro / 3, tamanhoBaga, tamanhoBaga);
                graficos.fillOval(centro, y + diametro / 3, tamanhoBaga, tamanhoBaga);
                graficos.fillOval(x + diametro - tamanhoBaga, y + diametro / 3, tamanhoBaga, tamanhoBaga);
                graficos.fillOval(centro, y + 2 * diametro / 3, tamanhoBaga, tamanhoBaga);
                break;
            case BANANA:
                java.awt.geom.Path2D.Double banana = new java.awt.geom.Path2D.Double();
                banana.moveTo(x + 2, y + 1);
                banana.curveTo(x + 1, y + diametro / 2, x + diametro / 2, y + diametro, x + diametro, y + diametro - 1);
                banana.curveTo(x + diametro - 2, y + diametro + 1, x + diametro / 2, y + diametro - 2, x + 2, y + 1);
                banana.closePath();
                graficos.fill(banana);
                break;
            case CEREJA:
                graficos.fillOval(x + 1, y + diametro / 3, diametro / 2 + 1, diametro / 2 + 1);
                graficos.fillOval(x + diametro / 2, y + diametro / 3, diametro / 2 + 1, diametro / 2 + 1);
                break;
            case KIWI:
                graficos.setColor(new Color(120, 72, 38));
                graficos.fillOval(x + 1, y + 1, diametro - 2, diametro - 2);
                graficos.setColor(new Color(163, 230, 53));
                graficos.fillOval(x + 3, y + 3, diametro - 6, diametro - 6);
                graficos.setColor(new Color(54, 45, 28));
                graficos.fillOval(x + diametro / 2, y + 3, 1, 1);
                graficos.fillOval(x + diametro / 2 - 2, y + diametro / 2, 1, 1);
                graficos.fillOval(x + diametro / 2 + 2, y + diametro / 2, 1, 1);
                graficos.fillOval(x + diametro / 2, y + diametro - 4, 1, 1);
                break;
            case ESPECIAL:
                graficos.fillPolygon(criarEstrela(x, y, diametro));
                break;
            default:
                throw new IllegalStateException("Tipo de fruta desconhecido.");
        }

        graficos.setColor(new Color(255, 255, 255, 150));
        if (tipo == TipoFruta.ESPECIAL) {
            graficos.fillOval(x + diametro / 2 - 1, y + diametro / 2 - 1, 2, 2);
        } else {
            graficos.fillOval(
                    x + diametro / 4,
                    y + diametro / 5,
                    Math.max(2, diametro / 5),
                    Math.max(2, diametro / 5));
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

    private Polygon criarEstrela(int x, int y, int diametro) {
        Polygon estrela = new Polygon();
        int centroX = x + diametro / 2;
        int centroY = y + diametro / 2;
        for (int ponto = 0; ponto < 10; ponto++) {
            double angulo = -Math.PI / 2 + ponto * Math.PI / 5;
            int raio = ponto % 2 == 0 ? diametro / 2 : diametro / 4;
            estrela.addPoint(
                    centroX + (int) Math.round(Math.cos(angulo) * raio),
                    centroY + (int) Math.round(Math.sin(angulo) * raio));
        }
        return estrela;
    }

    public enum TipoFruta {
        MACA(
                new Color(255, 166, 160),
                new Color(220, 38, 38),
                new Color(248, 113, 113, 35)),
        LARANJA(
                new Color(255, 213, 128),
                new Color(234, 88, 12),
                new Color(251, 146, 60, 38)),
        LIMAO(
                new Color(254, 240, 138),
                new Color(202, 138, 4),
                new Color(250, 204, 21, 38)),
        UVA(
                new Color(216, 180, 254),
                new Color(109, 40, 217),
                new Color(168, 85, 247, 42)),
        BANANA(
                new Color(254, 240, 138),
                new Color(202, 138, 4),
                new Color(250, 204, 21, 38)),
        CEREJA(
                new Color(252, 165, 165),
                new Color(185, 28, 28),
                new Color(239, 68, 68, 38)),
        KIWI(
                new Color(217, 180, 120),
                new Color(120, 72, 38),
                new Color(132, 204, 22, 38)),
        ESPECIAL(
                new Color(254, 243, 199),
                new Color(217, 119, 6),
                new Color(250, 204, 21, 65));

        private final Color corClara;
        private final Color corEscura;
        private final Color aura;

        TipoFruta(Color corClara, Color corEscura, Color aura) {
            this.corClara = corClara;
            this.corEscura = corEscura;
            this.aura = aura;
        }
    }
}
