package br.com.jogopoo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Ponto de entrada da aplicação Swing. */
public class Main {
    public static void main(String[] args) {
        // Cria a janela na Event Dispatch Thread, conforme exigido pelo Swing.
        SwingUtilities.invokeLater(() -> {
            // Adota o estilo do sistema quando disponível.
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | UnsupportedLookAndFeelException exception) {
                // Mantém o Look and Feel padrão do Swing se a configuração falhar.
                System.err.println("Não foi possível aplicar o visual nativo: "
                        + exception.getMessage());
            }
            new JogoCobrinha().setVisible(true);
        });
    }
}
