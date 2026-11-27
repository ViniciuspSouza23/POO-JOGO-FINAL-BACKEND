package br.com.jogopoo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Tenta integrar os componentes Swing à aparência do sistema operacional.
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | UnsupportedLookAndFeelException exception) {
                // Mantém a aplicação funcional caso o visual nativo não esteja disponível.
                System.err.println("Não foi possível aplicar o visual nativo: "
                        + exception.getMessage());
            }
            new JogoCobrinha().setVisible(true);
        });
    }
}
