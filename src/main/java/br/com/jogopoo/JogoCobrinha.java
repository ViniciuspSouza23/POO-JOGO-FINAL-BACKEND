package br.com.jogopoo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

/** Janela principal que hospeda o painel do jogo. */
public class JogoCobrinha extends JFrame {
    private static final long serialVersionUID = 1L;

    public JogoCobrinha() {
        setTitle("Jogo da Cobrinha");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new PainelJogo());
        setIconImage(criarIcone());
        pack();
        setLocationRelativeTo(null);
    }

    private BufferedImage criarIcone() {
        // Desenha um ícone independente de arquivos externos.
        BufferedImage icone = new BufferedImage(
                64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icone.createGraphics();
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(15, 23, 42));
        g2.fillRoundRect(0, 0, 64, 64, 16, 16);
        g2.setColor(new Color(74, 222, 128));
        g2.fillRoundRect(8, 8, 48, 48, 14, 14);
        g2.setStroke(new BasicStroke(5f));
        g2.setColor(new Color(15, 23, 42));
        g2.drawOval(20, 20, 24, 24);
        g2.setColor(Color.WHITE);
        g2.fillOval(39, 24, 6, 6);
        g2.fillOval(39, 34, 6, 6);
        g2.dispose();
        return icone;
    }
}
