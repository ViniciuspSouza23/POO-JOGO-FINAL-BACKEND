package br.com.jogopoo;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JColorChooser;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class PainelJogo extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int TAMANHO_CELULA = 20;
    private static final int COLUNAS = 30;
    private static final int LINHAS = 30;
    private static final int ALTURA_CABECALHO = 64;
    private static final int INTERVALO_RENDERIZACAO_MS = 16;
    private static final long PASSO_MOVIMENTO_NS = 120_000_000L;
    private static final Rectangle BOTAO_COR = new Rectangle(264, 14, 166, 36);
    private static final Color FUNDO = new Color(11, 18, 32);
    private static final Color CABECALHO = new Color(15, 23, 42);
    private static final Color TABULEIRO = new Color(17, 27, 46);
    private static final Color CELULA_ALTERNADA = new Color(20, 32, 52);
    private static final Color GRADE = new Color(26, 40, 61);
    private static final Color GRADE_PRINCIPAL = new Color(39, 57, 79);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color TEXTO_SECUNDARIO = new Color(148, 163, 184);
    private static final Color DESTAQUE = new Color(74, 222, 128);
    private static final Color CARTAO = new Color(20, 31, 51);

    private final Timer temporizador = new Timer(INTERVALO_RENDERIZACAO_MS, event -> atualizarJogo());
    private final List<ElementoJogo> elementos = new ArrayList<>();

    private Cobrinha cobrinha;
    private Comida comida;
    private Estado estado = Estado.INICIO;
    private int pontuacao;
    private boolean venceu;
    private Color corSelecionada = Cobrinha.COR_PADRAO;
    private boolean sobreBotaoCor;
    private long ultimoQuadroNs;
    private long tempoAcumuladoNs;

    public PainelJogo() {
        setLayout(new BorderLayout());
        setBackground(FUNDO);
        setPreferredSize(new Dimension(
                COLUNAS * TAMANHO_CELULA,
                LINHAS * TAMANHO_CELULA + ALTURA_CABECALHO));
        configurarTeclas();
        configurarSeletorDeCor();
    }

    private void configurarSeletorDeCor() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (BOTAO_COR.contains(event.getPoint())) {
                    escolherCor();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                sobreBotaoCor = false;
                setCursor(java.awt.Cursor.getDefaultCursor());
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                sobreBotaoCor = BOTAO_COR.contains(event.getPoint());
                setCursor(java.awt.Cursor.getPredefinedCursor(
                        sobreBotaoCor ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                repaint();
            }
        });
    }

    private void escolherCor() {
        boolean estavaJogando = estado == Estado.JOGANDO;
        if (estavaJogando) {
            temporizador.stop();
        }

        Color novaCor = JColorChooser.showDialog(this, "Escolha a cor da cobrinha", corSelecionada);
        if (novaCor != null) {
            corSelecionada = novaCor;
            if (cobrinha != null) {
                cobrinha.definirCor(novaCor);
            }
        }

        if (estavaJogando) {
            ultimoQuadroNs = System.nanoTime();
            temporizador.start();
        }
        repaint();
    }

    private void configurarTeclas() {
        associarTecla(KeyEvent.VK_UP, "cima", () -> mudarDirecao(Cobrinha.Direcao.CIMA));
        associarTecla(KeyEvent.VK_W, "cimaW", () -> mudarDirecao(Cobrinha.Direcao.CIMA));
        associarTecla(KeyEvent.VK_DOWN, "baixo", () -> mudarDirecao(Cobrinha.Direcao.BAIXO));
        associarTecla(KeyEvent.VK_S, "baixoS", () -> mudarDirecao(Cobrinha.Direcao.BAIXO));
        associarTecla(KeyEvent.VK_LEFT, "esquerda", () -> mudarDirecao(Cobrinha.Direcao.ESQUERDA));
        associarTecla(KeyEvent.VK_A, "esquerdaA", () -> mudarDirecao(Cobrinha.Direcao.ESQUERDA));
        associarTecla(KeyEvent.VK_RIGHT, "direita", () -> mudarDirecao(Cobrinha.Direcao.DIREITA));
        associarTecla(KeyEvent.VK_D, "direitaD", () -> mudarDirecao(Cobrinha.Direcao.DIREITA));
        associarTecla(KeyEvent.VK_SPACE, "iniciar", this::iniciarOuReiniciar);
    }

    private void associarTecla(int tecla, String nome, Runnable acao) {
        InputMap entrada = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap acoes = getActionMap();
        entrada.put(KeyStroke.getKeyStroke(tecla, 0), nome);
        acoes.put(nome, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                acao.run();
            }
        });
    }

    private void mudarDirecao(Cobrinha.Direcao direcao) {
        if (estado == Estado.JOGANDO && cobrinha != null) {
            cobrinha.mudarDirecao(direcao);
        }
    }

    private void iniciarOuReiniciar() {
        if (estado == Estado.INICIO || estado == Estado.FIM) {
            cobrinha = new Cobrinha(COLUNAS / 2, LINHAS / 2);
            cobrinha.definirCor(corSelecionada);
            comida = new Comida(0, 0);
            pontuacao = 0;
            venceu = false;
            estado = Estado.JOGANDO;
            posicionarComida();
            tempoAcumuladoNs = 0;
            ultimoQuadroNs = System.nanoTime();
            temporizador.start();
            repaint();
        }
    }

    private void atualizarJogo() {
        long agora = System.nanoTime();
        long decorrido = Math.max(0, agora - ultimoQuadroNs);
        ultimoQuadroNs = agora;
        tempoAcumuladoNs += Math.min(decorrido, PASSO_MOVIMENTO_NS);

        while (tempoAcumuladoNs >= PASSO_MOVIMENTO_NS && estado == Estado.JOGANDO) {
            tempoAcumuladoNs -= PASSO_MOVIMENTO_NS;
            moverUmaCelula();
        }
        repaint();
    }

    private void moverUmaCelula() {
        cobrinha.mover();

        if (cobrinha.getX() < 0 || cobrinha.getX() >= COLUNAS
                || cobrinha.getY() < 0 || cobrinha.getY() >= LINHAS
                || cobrinha.colidiuComProprioCorpo()) {
            estado = Estado.FIM;
            temporizador.stop();
            return;
        }

        if (cobrinha.getX() == comida.getX() && cobrinha.getY() == comida.getY()) {
            cobrinha.crescer();
            pontuacao += 10;
            posicionarComida();
        }
    }

    private void posicionarComida() {
        List<java.awt.Point> celulasLivres = new ArrayList<>();
        for (int y = 0; y < LINHAS; y++) {
            for (int x = 0; x < COLUNAS; x++) {
                if (!cobrinha.ocupa(x, y)) {
                    celulasLivres.add(new java.awt.Point(x, y));
                }
            }
        }

        if (celulasLivres.isEmpty()) {
            venceu = true;
            estado = Estado.FIM;
            temporizador.stop();
            return;
        }

        java.awt.Point posicao = celulasLivres.get(
                ThreadLocalRandom.current().nextInt(celulasLivres.size()));
        comida.reposicionar(posicao.x, posicao.y);
    }

    @Override
    protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        Graphics2D g2 = (Graphics2D) graficos.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharTabuleiro(g2);
        desenharElementos(g2);
        if (estado != Estado.JOGANDO) {
            desenharMensagem(g2);
        }

        g2.dispose();
    }

    private void desenharTabuleiro(Graphics2D g2) {
        g2.setColor(FUNDO);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(CABECALHO);
        g2.fillRect(0, 0, getWidth(), ALTURA_CABECALHO);
        g2.setColor(DESTAQUE);
        g2.fillRoundRect(18, 14, 34, 34, 11, 11);
        g2.setColor(new Color(11, 18, 32));
        g2.setFont(new Font("SansSerif", Font.BOLD, 19));
        g2.drawString("S", 29, 38);

        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("SNAKE", 62, 29);
        g2.setColor(TEXTO_SECUNDARIO);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString("SETAS / WASD PARA MOVER", 62, 46);

        g2.setColor(sobreBotaoCor ? new Color(42, 59, 80) : new Color(29, 43, 63));
        g2.fillRoundRect(BOTAO_COR.x, BOTAO_COR.y, BOTAO_COR.width, BOTAO_COR.height, 12, 12);
        g2.setColor(corSelecionada);
        g2.fillOval(BOTAO_COR.x + 10, BOTAO_COR.y + 9, 18, 18);
        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("TROCAR COR", BOTAO_COR.x + 37, BOTAO_COR.y + 22);

        int larguraCartao = 138;
        int xCartao = getWidth() - larguraCartao - 16;
        g2.setColor(new Color(29, 43, 63));
        g2.fillRoundRect(xCartao, 9, larguraCartao, 46, 12, 12);
        g2.setColor(TEXTO_SECUNDARIO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString("PONTUA\u00c7\u00c3O", xCartao + 12, 25);
        g2.setColor(DESTAQUE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.drawString(String.format("%03d", pontuacao), xCartao + 12, 46);

        int largura = COLUNAS * TAMANHO_CELULA;
        int altura = LINHAS * TAMANHO_CELULA;
        g2.setColor(TABULEIRO);
        g2.fillRect(0, ALTURA_CABECALHO, largura, altura);

        g2.setColor(CELULA_ALTERNADA);
        for (int y = 0; y < LINHAS; y++) {
            for (int x = 0; x < COLUNAS; x++) {
                if ((x + y) % 2 == 0) {
                    g2.fillRect(
                            x * TAMANHO_CELULA + 1,
                            ALTURA_CABECALHO + y * TAMANHO_CELULA + 1,
                            TAMANHO_CELULA - 1,
                            TAMANHO_CELULA - 1);
                }
            }
        }

        for (int x = 0; x <= COLUNAS; x++) {
            if (x % 5 == 0) {
                g2.setColor(GRADE_PRINCIPAL);
            } else {
                g2.setColor(GRADE);
            }
            int posicaoX = x * TAMANHO_CELULA;
            g2.drawLine(posicaoX, ALTURA_CABECALHO, posicaoX, ALTURA_CABECALHO + altura);
        }
        for (int y = 0; y <= LINHAS; y++) {
            if (y % 5 == 0) {
                g2.setColor(GRADE_PRINCIPAL);
            } else {
                g2.setColor(GRADE);
            }
            int posicaoY = ALTURA_CABECALHO + y * TAMANHO_CELULA;
            g2.drawLine(0, posicaoY, largura, posicaoY);
        }

        g2.setColor(new Color(46, 62, 87));
        g2.drawRect(0, ALTURA_CABECALHO, largura - 1, altura - 1);
    }

    private void desenharElementos(Graphics2D g2) {
        if (cobrinha == null) {
            return;
        }

        elementos.clear();
        elementos.add(cobrinha);
        elementos.add(comida);

        Graphics2D tabuleiro = (Graphics2D) g2.create();
        tabuleiro.translate(0, ALTURA_CABECALHO);
        double progresso = estado == Estado.JOGANDO
                ? (double) tempoAcumuladoNs / PASSO_MOVIMENTO_NS
                : 1.0;
        for (ElementoJogo elemento : elementos) {
            elemento.desenhar(tabuleiro, TAMANHO_CELULA, progresso);
        }
        tabuleiro.dispose();
    }

    private void desenharMensagem(Graphics2D g2) {
        int largura = COLUNAS * TAMANHO_CELULA;
        int altura = LINHAS * TAMANHO_CELULA;
        g2.setComposite(AlphaComposite.SrcOver.derive(0.68f));
        g2.setColor(FUNDO);
        g2.fill(new Rectangle2D.Double(0, ALTURA_CABECALHO, largura, altura));
        g2.setComposite(AlphaComposite.SrcOver);

        int cartaoLargura = 450;
        int cartaoAltura = 208;
        int cartaoX = (getWidth() - cartaoLargura) / 2;
        int cartaoY = ALTURA_CABECALHO + (altura - cartaoAltura) / 2;

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(cartaoX, cartaoY + 7, cartaoLargura, cartaoAltura, 24, 24);
        g2.setColor(CARTAO);
        g2.fillRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);
        g2.setColor(new Color(48, 66, 91));
        g2.drawRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);

        String titulo;
        String etiqueta;
        String detalhe;
        String instrucao;
        String dica;
        if (estado == Estado.INICIO) {
            titulo = "Jogo da Cobrinha";
            etiqueta = "ARCADE CL\u00c1SSICO";
            detalhe = "Pegue a comida e evite as colis\u00f5es.";
            instrucao = "PRESSIONE ESPA\u00c7O PARA COME\u00c7AR";
            dica = "Use as setas ou W A S D para se mover";
        } else if (venceu) {
            titulo = "Voc\u00ea venceu!";
            etiqueta = "TABULEIRO COMPLETO";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "ESPA\u00c7O PARA JOGAR NOVAMENTE";
            dica = "Incr\u00edvel! Voc\u00ea ocupou todo o tabuleiro.";
        } else {
            titulo = "Fim de jogo";
            etiqueta = "PARTIDA ENCERRADA";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "ESPA\u00c7O PARA JOGAR NOVAMENTE";
            dica = "Tente outra vez e supere sua pontua\u00e7\u00e3o.";
        }

        desenharTextoCentralizado(g2, etiqueta, cartaoY + 39, 10, Font.BOLD, DESTAQUE);
        desenharTextoCentralizado(g2, titulo, cartaoY + 79, 27, Font.BOLD, TEXTO);
        desenharTextoCentralizado(g2, detalhe, cartaoY + 106, 14, Font.PLAIN, TEXTO_SECUNDARIO);

        int botaoLargura = 322;
        int botaoX = (getWidth() - botaoLargura) / 2;
        int botaoY = cartaoY + 124;
        g2.setColor(new Color(36, 75, 58));
        g2.fillRoundRect(botaoX, botaoY, botaoLargura, 37, 12, 12);
        desenharTextoCentralizado(g2, instrucao, botaoY + 24, 11, Font.BOLD, DESTAQUE);
        desenharTextoCentralizado(g2, dica, cartaoY + 186, 12, Font.PLAIN, TEXTO_SECUNDARIO);
    }

    private void desenharTextoCentralizado(
            Graphics2D g2, String texto, int y, int tamanho, int estilo, Color cor) {
        g2.setFont(new Font("SansSerif", estilo, tamanho));
        g2.setColor(cor);
        FontMetrics metricas = g2.getFontMetrics();
        g2.drawString(texto, (getWidth() - metricas.stringWidth(texto)) / 2, y);
    }

    private enum Estado {
        INICIO,
        JOGANDO,
        FIM
    }
}
