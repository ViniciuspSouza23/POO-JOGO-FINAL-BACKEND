package br.com.jogopoo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cobrinha extends ElementoJogo {
    public static final Color COR_PADRAO = new Color(74, 222, 128);

    private final List<Point> corpo = new ArrayList<>();
    private List<Point> posicoesAnteriores = new ArrayList<>();
    private Color corCorpo = COR_PADRAO;
    private Color corCabeca = new Color(34, 197, 94);
    private Direcao direcao = Direcao.DIREITA;
    private boolean crescer;

    public Cobrinha(int x, int y) {
        super(x, y);
        corpo.add(new Point(x - 1, y));
        corpo.add(new Point(x - 2, y));
        posicoesAnteriores = capturarPosicoes();
    }

    public void mudarDirecao(Direcao novaDirecao) {
        // Bloqueia a inversão direta para impedir que a cobrinha colida consigo mesma.
        if (novaDirecao != direcao.oposta()) {
            direcao = novaDirecao;
        }
    }

    public void mover() {
        posicoesAnteriores = capturarPosicoes();
        corpo.add(0, new Point(getX(), getY()));
        // Mantém a cauda quando há crescimento; nos demais movimentos, remove seu último segmento.
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
        float[] hsb = Color.RGBtoHSB(cor.getRed(), cor.getGreen(), cor.getBlue(), null);
        float brilhoCabeca = hsb[2] > 0.65f
                ? Math.max(0.35f, hsb[2] * 0.75f)
                : Math.min(1.0f, hsb[2] + 0.25f);
        corCabeca = Color.getHSBColor(hsb[0], hsb[1], brilhoCabeca);
    }

    public boolean ocupa(int x, int y) {
        // Verifica primeiro a cabeça antes de procurar a posição no restante do corpo.
        if (getX() == x && getY() == y) {
            return true;
        }
        // Procura a coordenada entre todos os segmentos do corpo.
        for (Point segmento : corpo) {
            // Retorna assim que encontra um segmento na posição consultada.
            if (segmento.x == x && segmento.y == y) {
                return true;
            }
        }
        return false;
    }

    public boolean colidiuComProprioCorpo() {
        // Compara cada segmento com a posição atual da cabeça.
        for (Point segmento : corpo) {
            // Uma coincidência indica colisão com o próprio corpo.
            if (segmento.x == getX() && segmento.y == getY()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void desenhar(Graphics2D graficos, int tamanhoCelula, double progresso) {
        double fase = Math.max(0.0, Math.min(1.0, progresso));
        // Interpola os segmentos do corpo entre as posições dos quadros.
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

        // Posiciona os olhos de acordo com a direção atual da cabeça.
        switch (direcao) {
            // Olhos voltados para cima.
            case CIMA:
                olhoX1 = x + margem;
                olhoY1 = y + margem;
                olhoX2 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY2 = olhoY1;
                break;
            // Olhos voltados para baixo.
            case BAIXO:
                olhoX1 = x + margem;
                olhoY1 = y + tamanhoCelula - margem - tamanhoOlho;
                olhoX2 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY2 = olhoY1;
                break;
            // Olhos voltados para a esquerda.
            case ESQUERDA:
                olhoX1 = x + margem;
                olhoY1 = y + margem;
                olhoX2 = olhoX1;
                olhoY2 = y + tamanhoCelula - margem - tamanhoOlho;
                break;
            // Olhos voltados para a direita.
            case DIREITA:
                olhoX1 = x + tamanhoCelula - margem - tamanhoOlho;
                olhoY1 = y + margem;
                olhoX2 = olhoX1;
                olhoY2 = y + tamanhoCelula - margem - tamanhoOlho;
                break;
            // Protege contra qualquer valor de direção não tratado.
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
        // Copia a posição de cada segmento para a interpolação visual do próximo quadro.
        for (Point segmento : corpo) {
            posicoes.add(new Point(segmento));
        }
        return posicoes;
    }

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
            // Retorna a direção contrária para validar mudanças de sentido.
            switch (this) {
                // A direção oposta de cima é baixo.
                case CIMA:
                    return BAIXO;
                // A direção oposta de baixo é cima.
                case BAIXO:
                    return CIMA;
                // A direção oposta da esquerda é direita.
                case ESQUERDA:
                    return DIREITA;
                // A direção oposta da direita é esquerda.
                case DIREITA:
                    return ESQUERDA;
                // Protege contra qualquer direção não mapeada.
                default:
                    throw new IllegalStateException("Direção desconhecida.");
            }
        }
    }
}
