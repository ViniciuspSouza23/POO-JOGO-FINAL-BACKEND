package br.com.jogopoo;

import javax.swing.JFrame;

public class JogoCobrinha extends JFrame {
    private static final long serialVersionUID = 1L;

    public JogoCobrinha() {
        setTitle("Jogo da Cobrinha");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new PainelJogo());
        pack();
        setLocationRelativeTo(null);
    }
}
