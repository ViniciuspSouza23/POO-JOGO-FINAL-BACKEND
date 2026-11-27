package br.com.jogopoo;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

public class PainelJogo extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int TAMANHO_CELULA = 20;
    private static final int COLUNAS = 30;
    private static final int LINHAS = 30;
    private static final int ALTURA_CABECALHO = 64;
    private static final int INTERVALO_RENDERIZACAO_MS = 16;
    private static final long PASSO_MOVIMENTO_NS = 120_000_000L;
    private static final long DURACAO_ENTRADA_NS = 650_000_000L;
    private static final long DURACAO_FEEDBACK_NS = 450_000_000L;
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
    private final JButton botaoCor = criarBotaoCor();
    private final JButton botaoAcao = criarBotaoAcao();

    private Cobrinha cobrinha;
    private Comida comida;
    private Estado estado = Estado.INICIO;
    private int pontuacao;
    private boolean venceu;
    private Color corSelecionada = Cobrinha.COR_PADRAO;
    private Rectangle botaoIniciar;
    private long ultimoQuadroNs;
    private long tempoAcumuladoNs;
    private long inicioEntradaNs;
    private long inicioFeedbackPontuacaoNs;

    public PainelJogo() {
        setLayout(new BorderLayout());
        setBackground(FUNDO);
        setPreferredSize(new Dimension(
                COLUNAS * TAMANHO_CELULA,
                LINHAS * TAMANHO_CELULA + ALTURA_CABECALHO));
        setLayout(null);
        add(botaoCor);
        add(botaoAcao);
        botaoCor.setBounds(BOTAO_COR);
        botaoCor.setOpaque(false);
        botaoCor.setContentAreaFilled(false);
        botaoCor.setBorderPainted(false);
        botaoCor.setFocusable(true);
        botaoCor.setToolTipText("Escolher cor da cobrinha");
        botaoCor.getAccessibleContext().setAccessibleName("Trocar cor da cobrinha");

        botaoAcao.setBounds(139, 400, 322, 37);
        botaoAcao.setOpaque(false);
        botaoAcao.setContentAreaFilled(false);
        botaoAcao.setBorderPainted(false);
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

    private JButton criarBotaoAcao() {
        JButton botao = new JButton();
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return botao;
    }

    private void escolherCor() {
        boolean estavaJogando = estado == Estado.JOGANDO;
        // Pausa a partida enquanto o diálogo de seleção de cor estiver aberto.
        if (estavaJogando) {
            temporizador.stop();
        }

        Color novaCor = JColorChooser.showDialog(this, "Escolha a cor da cobrinha", corSelecionada);
        // Aplica apenas uma escolha confirmada pelo jogador.
        if (novaCor != null) {
            corSelecionada = novaCor;
            // Atualiza também a cobrinha existente, se a partida já tiver começado.
            if (cobrinha != null) {
                cobrinha.definirCor(novaCor);
            }
        }

        // Retoma o temporizador somente se a partida estava ativa antes do diálogo.
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
        associarTecla(KeyEvent.VK_ENTER, "iniciarEnter", this::iniciarOuReiniciar);
        associarTecla(KeyEvent.VK_P, "pausar", this::pausarOuRetomar);
        associarTecla(KeyEvent.VK_ESCAPE, "pausarEscape", this::pausarOuRetomar);
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
        // Ignora entradas de direção fora de uma partida ativa.
        if (estado == Estado.JOGANDO && cobrinha != null) {
            cobrinha.mudarDirecao(direcao);
        }
    }

    private void pausarOuRetomar() {
        // Interrompe a atualização e mostra o estado de pausa.
        if (estado == Estado.JOGANDO) {
            estado = Estado.PAUSADO;
            temporizador.stop();
            repaint();
        } else if (estado == Estado.PAUSADO) {
            // Retoma sem acumular o tempo passado durante a pausa.
            estado = Estado.JOGANDO;
            ultimoQuadroNs = System.nanoTime();
            tempoAcumuladoNs = 0;
            temporizador.start();
            repaint();
        }
    }

    private void iniciarOuReiniciar() {
        // Uma nova partida só pode começar na tela inicial ou após o fim da anterior.
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

        // Executa movimentos quando o tempo acumulado alcança o intervalo definido.
        while (tempoAcumuladoNs >= PASSO_MOVIMENTO_NS && estado == Estado.JOGANDO) {
            tempoAcumuladoNs -= PASSO_MOVIMENTO_NS;
            moverUmaCelula();
        }
        repaint();

        // Encerra o timer depois que a animação de entrada termina na tela inicial.
        if (estado == Estado.INICIO
                && agora - inicioEntradaNs >= DURACAO_ENTRADA_NS) {
            temporizador.stop();
        }
    }

    private void moverUmaCelula() {
        cobrinha.mover();

        // Finaliza a partida ao sair dos limites ou tocar o próprio corpo.
        if (cobrinha.getX() < 0 || cobrinha.getX() >= COLUNAS
                || cobrinha.getY() < 0 || cobrinha.getY() >= LINHAS
                || cobrinha.colidiuComProprioCorpo()) {
            estado = Estado.FIM;
            temporizador.stop();
            return;
        }

        // Cresce e pontua quando a cabeça alcança a comida.
        if (cobrinha.getX() == comida.getX() && cobrinha.getY() == comida.getY()) {
            cobrinha.crescer();
            pontuacao += 10;
            inicioFeedbackPontuacaoNs = System.nanoTime();
            posicionarComida();
        }
    }

    private void posicionarComida() {
        List<java.awt.Point> celulasLivres = new ArrayList<>();
        // Reúne as células que não estão ocupadas pela cobrinha.
        for (int y = 0; y < LINHAS; y++) {
            // Percorre as colunas de cada linha do tabuleiro.
            for (int x = 0; x < COLUNAS; x++) {
                // Adiciona somente células livres para a comida.
                if (!cobrinha.ocupa(x, y)) {
                    celulasLivres.add(new java.awt.Point(x, y));
                }
            }
        }

        // Se não houver espaço livre, a cobrinha ocupou o tabuleiro inteiro.
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
        // Exibe o cartão de estado fora da partida ativa.
        if (estado != Estado.JOGANDO) {
            desenharMensagem(g2);
        }

        g2.dispose();
    }

    private void desenharTabuleiro(Graphics2D g2) {
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
        g2.setColor(new Color(48, 65, 88));
        g2.drawLine(0, ALTURA_CABECALHO - 1, getWidth(), ALTURA_CABECALHO - 1);

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
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString("SETAS / WASD PARA MOVER", 62, 46);

        boolean sobreBotaoCor = botaoCor.getModel().isRollover();
        g2.setColor(sobreBotaoCor ? new Color(48, 70, 94) : new Color(29, 43, 63));
        g2.fillRoundRect(BOTAO_COR.x, BOTAO_COR.y, BOTAO_COR.width, BOTAO_COR.height, 12, 12);
        g2.setColor(sobreBotaoCor ? new Color(96, 122, 150) : new Color(48, 66, 91));
        g2.drawRoundRect(BOTAO_COR.x, BOTAO_COR.y, BOTAO_COR.width, BOTAO_COR.height, 12, 12);
        g2.setColor(corSelecionada);
        g2.fillOval(BOTAO_COR.x + 10, BOTAO_COR.y + 9, 18, 18);
        g2.setColor(brighter(corSelecionada));
        g2.fillOval(BOTAO_COR.x + 13, BOTAO_COR.y + 12, 5, 5);
        g2.setColor(TEXTO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("TROCAR COR", BOTAO_COR.x + 37, BOTAO_COR.y + 22);

        int larguraCartao = 138;
        int xCartao = getWidth() - larguraCartao - 16;
        g2.setColor(new Color(21, 34, 54));
        g2.fillRoundRect(xCartao, 9, larguraCartao, 46, 12, 12);
        g2.setColor(new Color(48, 66, 91));
        g2.drawRoundRect(xCartao, 9, larguraCartao, 46, 12, 12);
        g2.setColor(TEXTO_SECUNDARIO);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString("PONTUA\u00c7\u00c3O", xCartao + 12, 25);
        long tempoFeedback = inicioFeedbackPontuacaoNs == 0
                ? DURACAO_FEEDBACK_NS
                : System.nanoTime() - inicioFeedbackPontuacaoNs;
        // Aumenta brevemente o tamanho do número após ganhar pontos.
        if (tempoFeedback < DURACAO_FEEDBACK_NS) {
            float progresso = (float) tempoFeedback / DURACAO_FEEDBACK_NS;
            float escala = 1.0f + (1.0f - progresso) * 0.22f;
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.round(17 * escala)));
            g2.setColor(DESTAQUE);
            g2.drawString(String.format("%03d", pontuacao), xCartao + 12, 46);
        } else {
            // Mantém a pontuação no tamanho normal após o feedback.
            g2.setColor(DESTAQUE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 17));
            g2.drawString(String.format("%03d", pontuacao), xCartao + 12, 46);
        }
        g2.setColor(estado == Estado.JOGANDO ? DESTAQUE : TEXTO_SECUNDARIO);
        g2.fillOval(xCartao + larguraCartao - 24, 20, 8, 8);

        int largura = COLUNAS * TAMANHO_CELULA;
        int altura = LINHAS * TAMANHO_CELULA;
        GradientPaint tabuleiroGradiente = new GradientPaint(
                0, ALTURA_CABECALHO, new Color(20, 33, 54),
                0, ALTURA_CABECALHO + altura, new Color(13, 23, 40));
        g2.setPaint(tabuleiroGradiente);
        g2.fillRect(0, ALTURA_CABECALHO, largura, altura);

        g2.setColor(CELULA_ALTERNADA);
        // Desenha o padrão alternado das células do tabuleiro.
        for (int y = 0; y < LINHAS; y++) {
            // Percorre as células de cada linha.
            for (int x = 0; x < COLUNAS; x++) {
                // Alterna a cor de fundo para facilitar a leitura da grade.
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
        // Distribui pontos discretos para dar textura ao tabuleiro.
        for (int y = 0; y < LINHAS; y++) {
            // Examina cada célula para escolher posições da textura.
            for (int x = 0; x < COLUNAS; x++) {
                // Usa um padrão determinístico, evitando gerar ruído a cada quadro.
                if ((x * 7 + y * 13) % 19 == 0) {
                    g2.fillRect(
                            x * TAMANHO_CELULA + 9,
                            ALTURA_CABECALHO + y * TAMANHO_CELULA + 9,
                            2,
                            2);
                }
            }
        }

        // Desenha linhas verticais, destacando cada quinto intervalo.
        for (int x = 0; x <= COLUNAS; x++) {
            // Realça as divisões principais da grade.
            if (x % 5 == 0) {
                g2.setColor(GRADE_PRINCIPAL);
            } else {
                g2.setColor(GRADE);
            }
            int posicaoX = x * TAMANHO_CELULA;
            g2.drawLine(posicaoX, ALTURA_CABECALHO, posicaoX, ALTURA_CABECALHO + altura);
        }
        // Desenha linhas horizontais com o mesmo destaque periódico.
        for (int y = 0; y <= LINHAS; y++) {
            // Realça as divisões principais da grade.
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

        g2.setColor(new Color(74, 222, 128, 18));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(3, ALTURA_CABECALHO + 3, largura - 7, altura - 7);
    }

    private static Color brighter(Color cor) {
        return cor.brighter().brighter();
    }

    private void desenharElementos(Graphics2D g2) {
        // Não há elementos para desenhar antes do início da primeira partida.
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
        // Delega o desenho a cada elemento do jogo.
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

        // Anima a opacidade do cartão somente durante a entrada inicial.
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
        g2.setColor(CARTAO);
        g2.fillRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);
        g2.setColor(new Color(48, 66, 91));
        g2.drawRoundRect(cartaoX, cartaoY, cartaoLargura, cartaoAltura, 24, 24);
        g2.setComposite(AlphaComposite.SrcOver);

        String titulo;
        String etiqueta;
        String detalhe;
        String instrucao;
        String dica;
        // Prepara textos e instruções conforme o estado atual da partida.
        if (estado == Estado.INICIO) {
            titulo = "Jogo da Cobrinha";
            etiqueta = "ARCADE CL\u00c1SSICO";
            detalhe = "Pegue a comida e evite as colis\u00f5es.";
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Use as setas ou W A S D para se mover";
        } else if (estado == Estado.PAUSADO) {
            // Explica como retomar e mostra a pontuação atual durante a pausa.
            titulo = "Jogo pausado";
            etiqueta = "PARTIDA EM PAUSA";
            detalhe = "Pontua\u00e7\u00e3o atual: " + pontuacao;
            instrucao = "PRESSIONE P PARA CONTINUAR";
            dica = "Use as setas ou W A S D para se mover";
        } else if (venceu) {
            // Apresenta uma mensagem específica quando o jogador ocupa o tabuleiro.
            titulo = "Voc\u00ea venceu!";
            etiqueta = "TABULEIRO COMPLETO";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Incr\u00edvel! Voc\u00ea ocupou todo o tabuleiro.";
        } else {
            // Usa a mensagem de derrota para as demais partidas encerradas.
            titulo = "Fim de jogo";
            etiqueta = "PARTIDA ENCERRADA";
            detalhe = "Pontua\u00e7\u00e3o final: " + pontuacao;
            instrucao = "CLIQUE, ESPA\u00c7O OU ENTER";
            dica = "Tente outra vez e supere sua pontua\u00e7\u00e3o.";
        }

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
        boolean sobreBotaoAcao = botaoAcao.getModel().isRollover();
        g2.setColor(sobreBotaoAcao
                ? new Color(48, 112, 82)
                : new Color(36, 75, 58));
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

    private enum Estado {
        INICIO,
        JOGANDO,
        PAUSADO,
        FIM
    }
}
