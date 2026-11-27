package br.com.jogopoo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Coordena os estados da partida, os controles Swing e o desenho do tabuleiro.
 */
public class PainelJogo extends JPanel {
    private static final long serialVersionUID = 1L;

    // Geometria fixa do tabuleiro e intervalos de animação/movimento.
    private static final int TAMANHO_CELULA = 20;
    private static final int COLUNAS = 30;
    private static final int LINHAS = 30;
    private static final int ALTURA_CABECALHO = 64;
    private static final int INTERVALO_RENDERIZACAO_MS = 16;
    private static final long PASSO_MOVIMENTO_NS = 120_000_000L;
    private static final long PASSO_TURBO_NS = 75_000_000L;
    private static final long PASSO_LENTO_NS = 180_000_000L;
    private static final long DURACAO_PODER_NS = 5_000_000_000L;
    private static final long DURACAO_ENTRADA_NS = 650_000_000L;
    private static final long DURACAO_FEEDBACK_NS = 450_000_000L;
    private static final Rectangle BOTAO_COR = new Rectangle(216, 14, 104, 36);
    private static final Rectangle BOTAO_FUNDO = new Rectangle(326, 14, 104, 36);

    // Paleta compartilhada pelo fundo, tabuleiro, cartões e textos.
    private static final Color FUNDO = new Color(11, 18, 32);
    private static final Color CABECALHO = new Color(15, 23, 42);
    private static final Color TABULEIRO = new Color(17, 27, 46);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color TEXTO_SECUNDARIO = new Color(148, 163, 184);
    private static final Color DESTAQUE = new Color(74, 222, 128);
    private static final Color CARTAO = new Color(20, 31, 51);

    // O timer dirige a atualização na EDT; o movimento usa um intervalo próprio.
    private final Timer temporizador = new Timer(INTERVALO_RENDERIZACAO_MS, event -> atualizarJogo());
    // A referência ao tipo base permite desenhar subclasses por despacho polimórfico.
    private final List<ElementoJogo> elementos = new ArrayList<>();
    private final JButton botaoCor = criarBotaoCor();
    private final JButton botaoFundo = criarBotaoFundo();
    private final JButton botaoAcao = criarBotaoAcao();

    // Estado e dados da partida atual.
    private Cobrinha cobrinha;
    private Comida comida;
    private Estado estado = Estado.INICIO;
    private int pontuacao;
    private boolean venceu;
    private Color corSelecionada = Cobrinha.COR_PADRAO;
    private Color corFundoTabuleiro = TABULEIRO;
    private Rectangle botaoIniciar;

    // Referências temporais para movimento interpolado e feedback visual.
    private long ultimoQuadroNs;
    private long tempoAcumuladoNs;
    private long inicioEntradaNs;
    private long inicioFeedbackPontuacaoNs;
    private long turboRestanteNs;
    private long lentoRestanteNs;
    private long multiplicadorRestanteNs;
    private boolean escudoAtivo;

    public PainelJogo() {
        // Configura o tabuleiro, controles acessíveis e atualizações da tela inicial.
        setLayout(new BorderLayout());
        setBackground(FUNDO);
        setPreferredSize(new Dimension(
                COLUNAS * TAMANHO_CELULA,
                LINHAS * TAMANHO_CELULA + ALTURA_CABECALHO));
        setLayout(null);
        add(botaoCor);
        add(botaoFundo);
        add(botaoAcao);
        botaoCor.setBounds(BOTAO_COR);
        botaoCor.setOpaque(false);
        botaoCor.setContentAreaFilled(false);
        botaoCor.setBorderPainted(false);
        botaoCor.setFocusPainted(false);
        botaoCor.setFocusable(true);
        botaoCor.setToolTipText("Escolher cor da cobrinha");
        botaoCor.getAccessibleContext().setAccessibleName("Trocar cor da cobrinha");

        botaoFundo.setBounds(BOTAO_FUNDO);
        botaoFundo.setOpaque(false);
        botaoFundo.setContentAreaFilled(false);
        botaoFundo.setBorderPainted(false);
        botaoFundo.setFocusPainted(false);
        botaoFundo.setFocusable(true);
        botaoFundo.setToolTipText("Escolher cor de fundo do tabuleiro");
        botaoFundo.getAccessibleContext().setAccessibleName("Trocar cor de fundo do tabuleiro");

        botaoAcao.setBounds(139, 400, 322, 37);
        botaoAcao.setOpaque(false);
        botaoAcao.setContentAreaFilled(false);
        botaoAcao.setBorderPainted(false);
        botaoAcao.setFocusPainted(false);
        botaoAcao.setFocusable(true);
        botaoAcao.setVisible(false);
        botaoAcao.setToolTipText("Começar ou reiniciar a partida");
        botaoAcao.getAccessibleContext().setAccessibleName("Começar ou reiniciar partida");
        botaoAcao.addActionListener(event -> iniciarOuReiniciar());

        configurarTeclas();
        setFocusable(true);
        setToolTipText("Use as setas, W A S D, P ou Espa\u00e7o para jogar.");
        getAccessibleContext().setAccessibleDescription(
                "Jogo da cobrinha. Use setas ou W A S D para mover.");
        inicioEntradaNs = System.nanoTime();
        ultimoQuadroNs = inicioEntradaNs;
        temporizador.start();
    }

    private JButton criarBotaoCor() {
        JButton botao = new JButton();
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        botao.addActionListener(event -> escolherCor());
        return botao;
    }

    private JButton criarBotaoFundo() {
        JButton botao = new JButton();
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        botao.addActionListener(event -> escolherCorFundo());
        return botao;
    }

    private JButton criarBotaoAcao() {
        JButton botao = new JButton();
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return botao;
    }

    private void escolherCor() {
        // Preserva o estado da partida enquanto o seletor modal de cor está aberto.
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

    private void escolherCorFundo() {
        boolean estavaJogando = estado == Estado.JOGANDO;
        if (estavaJogando) {
            temporizador.stop();
        }

        Color novaCor = JColorChooser.showDialog(
                this, "Escolha a cor de fundo do tabuleiro", corFundoTabuleiro);
        if (novaCor != null) {
            corFundoTabuleiro = novaCor;
        }

        if (estavaJogando) {
            ultimoQuadroNs = System.nanoTime();
            temporizador.start();
        }
        repaint();
    }

    private void configurarTeclas() {
        // Associa setas/WASD ao movimento e atalhos dedicados a iniciar e pausar.
        associarTecla(KeyEvent.VK_UP, "cima", () -> mudarDirecao(Cobrinha.Direcao.CIMA));
        associarTecla(KeyEvent.VK_W, "cimaW", () -> mudarDirecao(Cobrinha.Direcao.CIMA));
        associarTecla(KeyEvent.VK_DOWN, "baixo", () -> mudarDirecao(Cobrinha.Direcao.BAIXO));
        associarTecla(KeyEvent.VK_S, "baixoS", () -> mudarDirecao(Cobrinha.Direcao.BAIXO));
        associarTecla(KeyEvent.VK_LEFT, "esquerda", () -> mudarDirecao(Cobrinha.Direcao.ESQUERDA));
        associarTecla(KeyEvent.VK_A, "esquerdaA", () -> mudarDirecao(Cobrinha.Direcao.ESQUERDA));
        associarTecla(KeyEvent.VK_RIGHT, "direita", () -> mudarDirecao(Cobrinha.Direcao.DIREITA));
        associarTecla(KeyEvent.VK_D, "direitaD", () -> mudarDirecao(Cobrinha.Direcao.DIREITA));
        associarTecla(KeyEvent.VK_SPACE, "iniciar", this::iniciarOuReiniciar);
        associarTecla(KeyEvent.VK_ENTER, "iniciarEnter", this::iniciarOuReiniciar);
        associarTecla(KeyEvent.VK_P, "pausar", this::pausarOuRetomar);
        associarTecla(KeyEvent.VK_ESCAPE, "pausarEscape", this::pausarOuRetomar);
    }

    private void associarTecla(int tecla, String nome, Runnable acao) {
        // Usa key bindings para manter os atalhos ativos mesmo com foco nos botões.
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
        // Ignora entradas de direção fora de uma partida ativa.
        if (estado == Estado.JOGANDO && cobrinha != null) {
            cobrinha.mudarDirecao(direcao);
        }
    }

    private void pausarOuRetomar() {
        // Alterna entre jogo e pausa sem alterar a posição ou a pontuação.
        if (estado == Estado.JOGANDO) {
            estado = Estado.PAUSADO;
            temporizador.stop();
            repaint();
        } else if (estado == Estado.PAUSADO) {
            estado = Estado.JOGANDO;
            ultimoQuadroNs = System.nanoTime();
            tempoAcumuladoNs = 0;
            temporizador.start();
            repaint();
        }
    }

    private void iniciarOuReiniciar() {
        // Reinicializa os dados somente ao iniciar uma nova partida ou após o fim.
        if (estado == Estado.INICIO || estado == Estado.FIM) {
            cobrinha = new Cobrinha(COLUNAS / 2, LINHAS / 2);
            cobrinha.definirCor(corSelecionada);
            comida = new Comida(0, 0);
            pontuacao = 0;
            venceu = false;
            turboRestanteNs = 0;
            lentoRestanteNs = 0;
            multiplicadorRestanteNs = 0;
            escudoAtivo = false;
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
        long tempoSimulado = Math.min(decorrido, PASSO_MOVIMENTO_NS);
        turboRestanteNs = Math.max(0, turboRestanteNs - tempoSimulado);
        lentoRestanteNs = Math.max(0, lentoRestanteNs - tempoSimulado);
        multiplicadorRestanteNs = Math.max(0, multiplicadorRestanteNs - tempoSimulado);
        tempoAcumuladoNs += Math.min(decorrido, intervaloMovimentoNs());

        // Converte o tempo acumulado em passos discretos de movimento.
        while (tempoAcumuladoNs >= intervaloMovimentoNs() && estado == Estado.JOGANDO) {
            tempoAcumuladoNs -= intervaloMovimentoNs();
            moverUmaCelula();
        }
        repaint();

        // Para as atualizações periódicas quando a animação inicial já terminou.
        if (estado == Estado.INICIO
                && agora - inicioEntradaNs >= DURACAO_ENTRADA_NS) {
            temporizador.stop();
        }
    }

    private void moverUmaCelula() {
        cobrinha.mover();

        // Aplica primeiro as condições de derrota antes de verificar a comida.
        boolean colidiuNaParede = cobrinha.getX() < 0 || cobrinha.getX() >= COLUNAS
                || cobrinha.getY() < 0 || cobrinha.getY() >= LINHAS;
        boolean colidiuComOCorpo = cobrinha.colidiuComProprioCorpo();
        if (colidiuNaParede || colidiuComOCorpo) {
            if (escudoAtivo) {
                escudoAtivo = false;
                if (colidiuNaParede) {
                    cobrinha.envolverNasBordas(COLUNAS, LINHAS);
                }
            } else {
                estado = Estado.FIM;
                temporizador.stop();
                return;
            }
        }

        // Comer aumenta a cobrinha e a pontuação, e reposiciona a comida.
        if (cobrinha.getX() == comida.getX() && cobrinha.getY() == comida.getY()) {
            Comida.TipoFruta frutaConsumida = comida.getTipo();
            long agora = System.nanoTime();
            cobrinha.crescer(
                    frutaConsumida == Comida.TipoFruta.MACA
                            || frutaConsumida == Comida.TipoFruta.ESPECIAL
                            ? 2
                            : 1);
            switch (frutaConsumida) {
                case LARANJA:
                    turboRestanteNs = DURACAO_PODER_NS;
                    lentoRestanteNs = 0;
                    break;
                case LIMAO:
                    escudoAtivo = true;
                    break;
                case UVA:
                    multiplicadorRestanteNs = DURACAO_PODER_NS;
                    break;
                case BANANA:
                    turboRestanteNs = 0;
                    lentoRestanteNs = DURACAO_PODER_NS;
                    break;
                case CEREJA:
                    break;
                case KIWI:
                    cobrinha.encolher(2);
                    break;
                case ESPECIAL:
                    turboRestanteNs = DURACAO_PODER_NS;
                    lentoRestanteNs = 0;
                    escudoAtivo = true;
                    multiplicadorRestanteNs = DURACAO_PODER_NS;
                    break;
                case MACA:
                    break;
                default:
                    throw new IllegalStateException("Tipo de fruta desconhecido.");
            }
            int pontosBase = frutaConsumida == Comida.TipoFruta.CEREJA ? 40 : 10;
            pontuacao += pontosBase * (multiplicadorRestanteNs > 0 ? 2 : 1);
            inicioFeedbackPontuacaoNs = agora;
            posicionarComida();
        }
    }

    private long intervaloMovimentoNs() {
        if (turboRestanteNs > 0) {
            return PASSO_TURBO_NS;
        }
        return lentoRestanteNs > 0 ? PASSO_LENTO_NS : PASSO_MOVIMENTO_NS;
    }

    private String textoStatusPoder() {
        if (estado != Estado.JOGANDO && estado != Estado.PAUSADO) {
            return "SETAS / WASD";
        }

        StringBuilder status = new StringBuilder();
        if (turboRestanteNs > 0) {
            status.append("TURBO ").append(segundosRestantes(turboRestanteNs)).append('s');
        }
        if (lentoRestanteNs > 0) {
            if (status.length() > 0) {
                status.append("  |  ");
            }
            status.append("LENTO ").append(segundosRestantes(lentoRestanteNs)).append('s');
        }
        if (multiplicadorRestanteNs > 0) {
            if (status.length() > 0) {
                status.append("  |  ");
            }
            status.append("2X ").append(segundosRestantes(multiplicadorRestanteNs)).append('s');
        }
        if (escudoAtivo) {
            if (status.length() > 0) {
                status.append("  |  ");
            }
            status.append("ESCUDO");
        }
        return status.length() == 0 ? "SETAS / WASD" : status.toString();
    }

    private static int segundosRestantes(long nanos) {
        return (int) Math.ceil(nanos / 1_000_000_000.0);
    }

    private void posicionarComida() {
        List<java.awt.Point> celulasLivres = new ArrayList<>();
        // Busca as posições disponíveis para evitar que a comida apareça sob a cobrinha.
        for (int y = 0; y < LINHAS; y++) {
            for (int x = 0; x < COLUNAS; x++) {
                if (!cobrinha.ocupa(x, y)) {
                    celulasLivres.add(new java.awt.Point(x, y));
                }
            }
        }

        // A ausência de células livres significa que a partida foi vencida.
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

        // Mantém a pintura dividida entre cenário, elementos e sobreposição de estado.
        desenharTabuleiro(g2);
        desenharElementos(g2);
        if (estado != Estado.JOGANDO) {
            desenharMensagem(g2);
        }

        g2.dispose();
    }

    private void desenharTabuleiro(Graphics2D g2) {
        // Renderiza a moldura, cabeçalho, pontuação e grade em camadas.
        GradientPaint fundo = new GradientPaint(
                0, 0, new Color(13, 21, 38),
                0, getHeight(), new Color(7, 12, 24));
        g2.setPaint(fundo);
        g2.fillRect(0, 0, getWidth(), getHeight());

        GradientPaint cabecalho = new GradientPaint(
                0, 0, new Color(20, 31, 52),
                getWidth(), 0, new Color(12, 20, 36));
        g2.setPaint(cabecalho);
        g2.fillRect(0, 0, getWidth(), ALTURA_CABECALHO);

        g2.setColor(new Color(74, 222, 128, 24));
        g2.fillOval(8, 4, 54, 54);
        g2.setColor(DESTAQUE);
        g2.fillRoundRect(18, 14, 34, 34, 11, 11);
        g2.setColor(new Color(11, 18, 32));
        g2.setFont(new Font("SansSerif", Font.BOLD, 19));
        g2.drawString("S", 29, 38);

        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("SNAKE", 62, 29);
        g2.setColor(TEXTO_SECUNDARIO);
        String statusPoder = textoStatusPoder();
        g2.setFont(new Font(
                "SansSerif",
                Font.PLAIN,
                statusPoder.equals("SETAS / WASD") ? 11 : 9));
        g2.drawString(statusPoder, 62, 46);

        boolean sobreBotaoCor = botaoCor.getModel().isRollover() || botaoCor.hasFocus();
        g2.setColor(sobreBotaoCor ? new Color(48, 70, 94) : new Color(29, 43, 63));
        g2.fillRoundRect(BOTAO_COR.x, BOTAO_COR.y, BOTAO_COR.width, BOTAO_COR.height, 12, 12);
        g2.setColor(sobreBotaoCor ? new Color(96, 122, 150) : new Color(48, 66, 91));
        g2.drawRoundRect(BOTAO_COR.x, BOTAO_COR.y, BOTAO_COR.width, BOTAO_COR.height, 12, 12);
        g2.setColor(corSelecionada);
        g2.fillOval(BOTAO_COR.x + 10, BOTAO_COR.y + 9, 18, 18);
        g2.setColor(ajustarContraste(corSelecionada, 0.65f));
        g2.drawOval(BOTAO_COR.x + 9, BOTAO_COR.y + 8, 20, 20);
        g2.setColor(brighter(corSelecionada));
        g2.fillOval(BOTAO_COR.x + 13, BOTAO_COR.y + 12, 5, 5);
        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("COBRINHA", BOTAO_COR.x + 35, BOTAO_COR.y + 22);

        boolean sobreBotaoFundo = botaoFundo.getModel().isRollover() || botaoFundo.hasFocus();
        g2.setColor(sobreBotaoFundo ? new Color(48, 70, 94) : new Color(29, 43, 63));
        g2.fillRoundRect(
                BOTAO_FUNDO.x, BOTAO_FUNDO.y,
                BOTAO_FUNDO.width, BOTAO_FUNDO.height, 12, 12);
        g2.setColor(sobreBotaoFundo ? new Color(96, 122, 150) : new Color(48, 66, 91));
        g2.drawRoundRect(
                BOTAO_FUNDO.x, BOTAO_FUNDO.y,
                BOTAO_FUNDO.width, BOTAO_FUNDO.height, 12, 12);
        int amostraX = BOTAO_FUNDO.x + 9;
        int amostraY = BOTAO_FUNDO.y + 9;
        g2.setColor(corFundoTabuleiro);
        g2.fillRoundRect(amostraX, amostraY, 17, 18, 4, 4);
        g2.setColor(ajustarContraste(corFundoTabuleiro, 0.65f));
        g2.drawRoundRect(amostraX, amostraY, 17, 18, 4, 4);
        g2.drawLine(amostraX + 5, amostraY + 3, amostraX + 5, amostraY + 15);
        g2.drawLine(amostraX + 11, amostraY + 3, amostraX + 11, amostraY + 15);
        g2.drawLine(amostraX + 3, amostraY + 6, amostraX + 14, amostraY + 6);
        g2.drawLine(amostraX + 3, amostraY + 12, amostraX + 14, amostraY + 12);
        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("FUNDO", BOTAO_FUNDO.x + 34, BOTAO_FUNDO.y + 22);

        int larguraCartao = 138;
        int xCartao = getWidth() - larguraCartao - 16;
        g2.setPaint(new GradientPaint(
                xCartao, 9, new Color(30, 47, 70),
                xCartao, 55, new Color(18, 30, 49)));
        g2.fillRoundRect(xCartao, 9, larguraCartao, 46, 12, 12);
        g2.setColor(new Color(48, 66, 91));
        g2.drawRoundRect(xCartao, 9, larguraCartao, 46, 12, 12);
        g2.setColor(TEXTO_SECUNDARIO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString("PONTUA\u00c7\u00c3O", xCartao + 12, 25);
        long tempoFeedback = inicioFeedbackPontuacaoNs == 0
                ? DURACAO_FEEDBACK_NS
                : System.nanoTime() - inicioFeedbackPontuacaoNs;
        // Dá destaque temporário à pontuação recém-atualizada.
        if (tempoFeedback < DURACAO_FEEDBACK_NS) {
            float progresso = (float) tempoFeedback / DURACAO_FEEDBACK_NS;
            float escala = 1.0f + (1.0f - progresso) * 0.22f;
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.round(17 * escala)));
            g2.setColor(DESTAQUE);
            g2.drawString(String.format("%03d", pontuacao), xCartao + 12, 46);
        } else {
            g2.setColor(DESTAQUE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 17));
            g2.drawString(String.format("%03d", pontuacao), xCartao + 12, 46);
        }
        g2.setColor(estado == Estado.JOGANDO
                ? new Color(74, 222, 128, 36)
                : new Color(148, 163, 184, 28));
        g2.fillOval(xCartao + larguraCartao - 29, 15, 18, 18);
        g2.setColor(estado == Estado.JOGANDO ? DESTAQUE : TEXTO_SECUNDARIO);
        g2.fillOval(xCartao + larguraCartao - 24, 20, 8, 8);

        int largura = COLUNAS * TAMANHO_CELULA;
        int altura = LINHAS * TAMANHO_CELULA;
        Color celulaAlternada = ajustarContraste(corFundoTabuleiro, 0.025f);
        Color corGrade = ajustarContraste(corFundoTabuleiro, 0.05f);
        Color corGradePrincipal = ajustarContraste(corFundoTabuleiro, 0.10f);
        GradientPaint tabuleiroGradiente = new GradientPaint(
                0, ALTURA_CABECALHO, ajustarContraste(corFundoTabuleiro, 0.035f),
                0, ALTURA_CABECALHO + altura, ajustarContraste(corFundoTabuleiro, 0.16f));
        g2.setPaint(tabuleiroGradiente);
        g2.fillRect(0, ALTURA_CABECALHO, largura, altura);

        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(largura / 2f, ALTURA_CABECALHO + altura / 2f),
                largura * 0.78f,
                new float[] {0f, 1f},
                new Color[] {
                    new Color(74, 222, 128, 22),
                    new Color(74, 222, 128, 0)
                }));
        g2.fillRect(0, ALTURA_CABECALHO, largura, altura);

        g2.setColor(celulaAlternada);
        // Separa visualmente as células sem competir com os elementos do jogo.
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

        g2.setColor(new Color(255, 255, 255, 8));
        // Aplica uma textura discreta e determinística ao tabuleiro.
        for (int y = 0; y < LINHAS; y++) {
            for (int x = 0; x < COLUNAS; x++) {
                if ((x * 7 + y * 13) % 19 == 0) {
                    g2.fillRect(
                            x * TAMANHO_CELULA + 9,
                            ALTURA_CABECALHO + y * TAMANHO_CELULA + 9,
                            2,
                            2);
                }
            }
        }

        // Desenha a grade com linhas principais a cada cinco células.
        for (int x = 0; x <= COLUNAS; x++) {
            if (x % 5 == 0) {
                g2.setColor(corGradePrincipal);
            } else {
                g2.setColor(corGrade);
            }
            int posicaoX = x * TAMANHO_CELULA;
            g2.drawLine(posicaoX, ALTURA_CABECALHO, posicaoX, ALTURA_CABECALHO + altura);
        }
        for (int y = 0; y <= LINHAS; y++) {
            if (y % 5 == 0) {
                g2.setColor(corGradePrincipal);
            } else {
                g2.setColor(corGrade);
            }
            int posicaoY = ALTURA_CABECALHO + y * TAMANHO_CELULA;
            g2.drawLine(0, posicaoY, largura, posicaoY);
        }

        g2.setColor(new Color(46, 62, 87));
        g2.drawRect(0, ALTURA_CABECALHO, largura - 1, altura - 1);

        g2.setColor(new Color(74, 222, 128, 18));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(3, ALTURA_CABECALHO + 3, largura - 7, altura - 7);
    }

    private static Color brighter(Color cor) {
        return cor.brighter().brighter();
    }

    private static Color ajustarContraste(Color cor, float intensidade) {
        float brilho = cor.getRed() * 0.2126f
                + cor.getGreen() * 0.7152f
                + cor.getBlue() * 0.0722f;
        int alvo = brilho < 128 ? 255 : 0;
        return new Color(
                Math.round(cor.getRed() + (alvo - cor.getRed()) * intensidade),
                Math.round(cor.getGreen() + (alvo - cor.getGreen()) * intensidade),
                Math.round(cor.getBlue() + (alvo - cor.getBlue()) * intensidade));
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
                ? (double) tempoAcumuladoNs / intervaloMovimentoNs()
                : 1.0;
        // Usa polimorfismo para desenhar a cobrinha e a comida no espaço do tabuleiro.
        for (ElementoJogo elemento : elementos) {
            elemento.desenhar(tabuleiro, TAMANHO_CELULA, progresso);
        }
        tabuleiro.dispose();
    }

    private void desenharMensagem(Graphics2D g2) {
        // Sobrepõe o tabuleiro com um cartão de início, pausa, vitória ou derrota.
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

        // Suaviza a entrada do cartão sem animar os outros estados.
        if (estado == Estado.INICIO) {
            long tempoEntrada = Math.max(
                    0L,
                    System.nanoTime() - inicioEntradaNs);
            float progressoEntrada = (float) Math.min(
                    1.0,
                    (double) tempoEntrada / DURACAO_ENTRADA_NS);
            float suavizado = progressoEntrada * progressoEntrada
                    * (3.0f - 2.0f * progressoEntrada);
            g2.setComposite(AlphaComposite.SrcOver.derive(
                    0.15f + 0.85f * suavizado));
        }

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(cartaoX, cartaoY + 7, cartaoLargura, cartaoAltura, 24, 24);
        g2.setPaint(new GradientPaint(
                cartaoX, cartaoY, new Color(28, 44, 66),
                cartaoX, cartaoY + cartaoAltura, CARTAO));
        g2.fillRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);
        g2.setColor(new Color(48, 66, 91));
        g2.drawRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);
        g2.setComposite(AlphaComposite.SrcOver);

        String titulo;
        String etiqueta;
        String detalhe;
        String instrucao;
        String dica;
        // Seleciona o conteúdo do cartão a partir do estado atual da partida.
        if (estado == Estado.INICIO) {
            titulo = "Jogo da Cobrinha";
            etiqueta = "ARCADE CL\u00c1SSICO";
            detalhe = "Pegue a comida e evite as colis\u00f5es.";
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Use as setas ou W A S D para se mover";
        } else if (estado == Estado.PAUSADO) {
            titulo = "Jogo pausado";
            etiqueta = "PARTIDA EM PAUSA";
            detalhe = "Pontua\u00e7\u00e3o atual: " + pontuacao;
            instrucao = "PRESSIONE P PARA CONTINUAR";
            dica = "Use as setas ou W A S D para se mover";
        } else if (venceu) {
            titulo = "Voc\u00ea venceu!";
            etiqueta = "TABULEIRO COMPLETO";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Incr\u00edvel! Voc\u00ea ocupou todo o tabuleiro.";
        } else {
            titulo = "Fim de jogo";
            etiqueta = "PARTIDA ENCERRADA";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Tente outra vez e supere sua pontua\u00e7\u00e3o.";
        }

        g2.setColor(new Color(74, 222, 128, 18));
        g2.fillRoundRect((getWidth() - 166) / 2, cartaoY + 22, 166, 24, 12, 12);
        desenharTextoCentralizado(g2, etiqueta, cartaoY + 39, 11, Font.BOLD, DESTAQUE);
        desenharTextoCentralizado(g2, titulo, cartaoY + 79, 27, Font.BOLD, TEXTO);
        desenharTextoCentralizado(g2, detalhe, cartaoY + 106, 14, Font.PLAIN, TEXTO_SECUNDARIO);

        int botaoLargura = 322;
        int botaoX = (getWidth() - botaoLargura) / 2;
        int botaoY = cartaoY + 124;
        botaoIniciar = new Rectangle(botaoX, botaoY, botaoLargura, 37);
        botaoAcao.setBounds(botaoIniciar);
        botaoAcao.setVisible(estado != Estado.JOGANDO && estado != Estado.PAUSADO);
        botaoAcao.setText("");
        boolean sobreBotaoAcao = botaoAcao.getModel().isRollover() || botaoAcao.hasFocus();
        g2.setColor(new Color(74, 222, 128, sobreBotaoAcao ? 30 : 16));
        g2.fillRoundRect(botaoX - 3, botaoY - 3, botaoLargura + 6, 43, 15, 15);
        g2.setPaint(new GradientPaint(
                botaoX, botaoY,
                sobreBotaoAcao ? new Color(48, 112, 82) : new Color(42, 91, 67),
                botaoX, botaoY + 37,
                sobreBotaoAcao ? new Color(36, 75, 58) : new Color(28, 60, 48)));
        g2.fillRoundRect(botaoX, botaoY, botaoLargura, 37, 12, 12);
        g2.setColor(sobreBotaoAcao
                ? new Color(108, 190, 146)
                : new Color(74, 140, 106));
        g2.drawRoundRect(botaoX, botaoY, botaoLargura, 37, 12, 12);
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

    /** Estados que determinam os controles e a apresentação da partida. */
    private enum Estado {
        INICIO,
        JOGANDO,
        PAUSADO,
        FIM
    }
}
