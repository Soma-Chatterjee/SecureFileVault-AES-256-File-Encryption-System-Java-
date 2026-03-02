import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

/**
 * Swing-based front end for the SecureFileVault application.
 * Provides a polished interactive interface with drag-and-drop, status bar,
 * progress indicator, password visibility toggle and responsive background
 * operations.
 */
public class SecureFileVaultGUI extends JFrame {

    private JTextField filePathField;
    private JPasswordField passwordField;
    private JCheckBox showPassword;
    private JButton browseButton;
    private JButton encryptButton;
    private JButton decryptButton;
    private JFileChooser fileChooser;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public SecureFileVaultGUI() {
        setTitle("Secure File Vault");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 220);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        initComponents();
    }

    private void initComponents() {
        filePathField = new JTextField(30);
        passwordField = new JPasswordField(30);
        showPassword = new JCheckBox("Show");
        browseButton = new JButton("Browse...");
        encryptButton = new JButton("Encrypt");
        decryptButton = new JButton("Decrypt");
        statusLabel = new JLabel("Ready");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);

        fileChooser = new JFileChooser();

        // allow drag-and-drop of files onto the text field
        new DropTarget(filePathField, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    Object data = evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (data instanceof List) {
                        List<?> files = (List<?>) data;
                        if (!files.isEmpty() && files.get(0) instanceof File) {
                            filePathField.setText(((File) files.get(0)).getAbsolutePath());
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }, true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("File:"), gbc);

        gbc.gridx = 1;
        panel.add(filePathField, gbc);

        gbc.gridx = 2;
        panel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 2;
        panel.add(showPassword, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(encryptButton, gbc);

        gbc.gridx = 1;
        panel.add(decryptButton, gbc);

        // status row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        panel.add(statusLabel, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 4;
        panel.add(progressBar, gbc);

        add(panel);

        browseButton.addActionListener(e -> browseFile());
        encryptButton.addActionListener(e -> doEncrypt());
        decryptButton.addActionListener(e -> doDecrypt());

        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar((Character) UIManager.get("PasswordField.echoChar"));
            }
        });
    }

    private void browseFile() {
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void setBusy(boolean busy, String message) {
        encryptButton.setEnabled(!busy);
        decryptButton.setEnabled(!busy);
        browseButton.setEnabled(!busy);
        progressBar.setVisible(busy);
        statusLabel.setText(message);
    }

    private void doEncrypt() {
        String path = filePathField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (path.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file and enter a password.");
            return;
        }
        setBusy(true, "Encrypting...");
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    SecureFileVault.encryptFile(path, password);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Ready");
                try {
                    get();
                    JOptionPane.showMessageDialog(SecureFileVaultGUI.this, "Encryption completed.");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SecureFileVaultGUI.this, "Error: " + e.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }

    private void doDecrypt() {
        String path = filePathField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (path.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file and enter a password.");
            return;
        }
        setBusy(true, "Decrypting...");
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    SecureFileVault.decryptFile(path, password);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Ready");
                try {
                    get();
                    JOptionPane.showMessageDialog(SecureFileVaultGUI.this, "Decryption completed.");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SecureFileVaultGUI.this, "Error: " + e.getCause().getMessage());
                }
            }
        };
        worker.execute();
    }
}