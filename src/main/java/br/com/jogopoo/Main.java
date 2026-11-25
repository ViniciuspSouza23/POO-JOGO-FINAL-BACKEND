package br.com.jogopoo;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JogoCobrinha().setVisible(true));
    }
}
