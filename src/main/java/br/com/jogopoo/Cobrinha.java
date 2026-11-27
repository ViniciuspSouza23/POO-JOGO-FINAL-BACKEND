package br.com.jogopoo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementa a cobrinha como especialização de {@link ElementoJogo}.
 *
 * <p>Herda a posição comum, encapsula corpo e direção e sobrescreve o desenho
 * para fornecer sua representação específica no tabuleiro.</p>
 */
public class Cobrinha extends ElementoJogo {
    public static final Color COR_PADRAO = new Color(74, 222, 128);

    // Estado interno encapsulado; os segmentos seguem da cabeça em direção à cauda.
    private final List<Point> corpo = new ArrayList<>();
    // Cópia do quadro anterior usada para interpolar o movimento na renderização.
    private List<Point> posicoesAnteriores = new ArrayList<>();
    private Color corCorpo = COR_PADRAO;
    private Color corCabeca = new Color(34, 197, 94);
    private Direcao direcao = Direcao.DIREITA;
    // Sinaliza que o próximo movimento deve preservar o último segmento.
    private boolean crescer;

    public Cobrinha(int x, int y) {
        super(x, y);
        corpo.add(new Point(x - 1, y));
        corpo.add(new Point(x - 2, y));
        posicoesAnteriores = capturarPosicoes();
    }

    public void mudarDirecao(Direcao novaDirecao) {
        // Impede uma inversão imediata de sentido.
        if (novaDirecao != direcao.oposta()) {
            direcao = novaDirecao;
        }
    }

    public void mover() {
        // Guarda o quadro atual para animação, avança o corpo e move a cabeça uma célula.
        posicoesAnteriores = capturarPosicoes();
        corpo.add(0, new Point(getX(), getY()));
        if (crescer) {
            crescer = false;
        } else {
            corpo.remove(corpo.size() - 1);
        }
        definirPosicao(getX() + direcao.getDeltaX(), getY() + direcao.getDeltaY());
    }

    public void crescer() {
        crescer = true;
    }

    public Color getCor() {
        return corCorpo;
    }

    public void definirCor(Color cor) {
        corCorpo = Objects.requireNonNull(cor, "A cor da cobrinha não pode ser nula.");
        // Deriva uma tonalidade próxima para destacar visualmente a cabeça.
        float[] hsb = Color.RGBtoHSB(cor.getRed(), cor.getGreen(), cor.getBlue(), null);
        float brilhoCabeca = hsb[2] > 0.65f
                ? Math.max(0.35f, hsb[2] * 0.75f)
                : Math.min(1.0f, hsb[2] + 0.25f);
        corCabeca = Color.getHSBColor(hsb[0], hsb[1], brilhoCabeca);
    }

    public boolean ocupa(int x, int y) {
        if (getX() == x && getY() == y) {
            return true;
        }
        for (Point segmento : corpo) {
            if (segmento.x == x && segmento.y == y) {
                return true;
            }
        }
        return false;
    }

    public boolean colidiuComProprioCorpo() {
        for (Point segmento : corpo) {
            if (segmento.x == getX() && segmento.y == getY()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso) {
        double fase = Math.max(0.0, Math.min(1.0, progresso));
        // Desenha o corpo interpolado e, em seguida, a cabeça com seus detalhes visuais.
        for (int i = 0; i < corpo.size(); i++) {
            Point segmentoAtual = corpo.get(i);
            Point segmentoAnterior = i + 1 < posicoesAnteriores.size()
                    ? posicoesAnteriores.get(i + 1)
                    : segmentoAtual;
            desenharSegmento(graficos, segmentoAnterior, segmentoAtual, tamanhoCelula, fase);
        }

        Point cabecaAnterior = posicoesAnteriores.get(0);
        Point cabecaAtual = new Point(getX(), getY());
        double x = cabecaAnterior.x + (cabecaAtual.x - cabecaAnterior.x) * fase;
        double y = cabecaAnterior.y + (cabecaAtual.y - cabecaAnterior.y) * fase;
        int xPixel = (int) Math.round(x * tamanhoCelula) + 1;
        int yPixel = (int) Math.round(y * tamanhoCelula) + 1;
        graficos.setColor(new Color(5, 10, 20, 80));
        graficos.fillRoundRect(
                xPixel + 2,
                yPixel + 3,
                tamanhoCelula - 2,
                tamanhoCelula - 2,
                10,
                10);
        GradientPaint degradedeCabeca = new GradientPaint(
                xPixel, yPixel, corCorpo.brighter(),
                xPixel + tamanhoCelula, yPixel + tamanhoCelula, corCabeca);
        graficos.setPaint(degradedeCabeca);
        graficos.fillRoundRect(
                xPixel,
                yPixel,
                tamanhoCelula - 2,
                tamanhoCelula - 2,
                10,
                10);
        graficos.setColor(new Color(255, 255, 255, 70));
        graficos.setStroke(new BasicStroke(1.2f));
        graficos.drawRoundRect(
                xPixel + 2, yPixel + 2,
                tamanhoCelula - 6, tamanhoCelula - 6, 8, 8);
        desenharOlhos(graficos, xPixel, yPixel, tamanhoCelula);
    }

    private void desenharSegmento(
            Graphics2D graficos, Point anterior, Point atual, int tamanhoCelula, double fase) {
        double x = anterior.x + (atual.x - anterior.x) * fase;
        double y = anterior.y + (atual.y - anterior.y) * fase;
        int xPixel = (int) Math.round(x * tamanhoCelula) + 1;
        int yPixel = (int) Math.round(y * tamanhoCelula) + 1;

        graficos.setColor(new Color(5, 10, 20, 55));
        graficos.fillRoundRect(
                xPixel + 2,
                yPixel + 3,
                tamanhoCelula - 2,
                tamanhoCelula - 2,
                8,
                8);
        GradientPaint degradedeCorpo = new GradientPaint(
                xPixel, yPixel, corCorpo.brighter(),
                xPixel + tamanhoCelula, yPixel + tamanhoCelula, corCorpo);
        graficos.setPaint(degradedeCorpo);
        graficos.fillRoundRect(
                xPixel,
                yPixel,
                tamanhoCelula - 2,
                tamanhoCelula - 2,
                8,
                8);
        graficos.setColor(new Color(255, 255, 255, 45));
        graficos.fillRoundRect(
                xPixel + 4,
                yPixel + 3,
                tamanhoCelula - 9,
                3,
                3,
                3);
    }

    private void desenharOlhos(Graphics2D graficos, int x, int y, int tamanhoCelula) {
        int tamanhoOlho = Math.max(3, tamanhoCelula / 5);
        int margem = Math.max(3, tamanhoCelula / 4);
        int olhoX1;
        int olhoY1;
        int olhoX2;
        int olhoY2;

        // Ajusta a posição dos olhos para acompanhar a orientação da cabeça.
        switch (direcao) {
            case CIMA:
                olhoX1 = x + margem;
                olhoY1 = y + margem;
                olhoX2 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY2 = olhoY1;
                break;
            case BAIXO:
                olhoX1 = x + margem;
                olhoY1 = y + tamanhoCelula - margem - tamanhoOlho;
                olhoX2 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY2 = olhoY1;
                break;
            case ESQUERDA:
                olhoX1 = x + margem;
                olhoY1 = y + margem;
                olhoX2 = olhoX1;
                olhoY2 = y + tamanhoCelula - margem - tamanhoOlho;
                break;
            case DIREITA:
                olhoX1 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY1 = y + margem;
                olhoX2 = olhoX1;
                olhoY2 = y + tamanhoCelula - margem - tamanhoOlho;
                break;
            default:
                throw new IllegalStateException("Direção desconhecida.");
        }

        graficos.setColor(Color.WHITE);
        graficos.fillOval(olhoX1, olhoY1, tamanhoOlho, tamanhoOlho);
        graficos.fillOval(olhoX2, olhoY2, tamanhoOlho, tamanhoOlho);

        int tamanhoPupila = Math.max(2, tamanhoOlho / 2);
        int deslocamentoX = direcao == Direcao.DIREITA ? tamanhoOlho - tamanhoPupila
                : direcao == Direcao.ESQUERDA ? 0
                : (tamanhoOlho - tamanhoPupila) / 2;
        int deslocamentoY = direcao == Direcao.BAIXO ? tamanhoOlho - tamanhoPupila
                : direcao == Direcao.CIMA ? 0
                : (tamanhoOlho - tamanhoPupila) / 2;
        graficos.setColor(new Color(15, 23, 42));
        graficos.fillOval(
                olhoX1 + deslocamentoX, olhoY1 + deslocamentoY, tamanhoPupila, tamanhoPupila);
        graficos.fillOval(
                olhoX2 + deslocamentoX, olhoY2 + deslocamentoY, tamanhoPupila, tamanhoPupila);
    }

    private List<Point> capturarPosicoes() {
        List<Point> posicoes = new ArrayList<>(corpo.size() + 1);
        posicoes.add(new Point(getX(), getY()));
        // Inclui cabeça e segmentos para manter um snapshot independente do estado atual.
        for (Point segmento : corpo) {
            posicoes.add(new Point(segmento));
        }
        return posicoes;
    }

    /** Direções válidas de movimento e seus deslocamentos no tabuleiro. */
    public enum Direcao {
        CIMA(0, -1),
        BAIXO(0, 1),
        ESQUERDA(-1, 0),
        DIREITA(1, 0);

        private final int deltaX;
        private final int deltaY;

        Direcao(int deltaX, int deltaY) {
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        private int getDeltaX() {
            return deltaX;
        }

        private int getDeltaY() {
            return deltaY;
        }

        private Direcao oposta() {
            // Mapeia cada direção para sua oposta, usada para validar curvas.
            switch (this) {
                case CIMA:
                    return BAIXO;
                case BAIXO:
                    return CIMA;
                case ESQUERDA:
                    return DIREITA;
                case DIREITA:
                    return ESQUERDA;
                default:
                    throw new IllegalStateException("Direção desconhecida.");
            }
        }
    }
}
