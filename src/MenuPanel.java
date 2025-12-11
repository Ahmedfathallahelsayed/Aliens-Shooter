import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class MenuPanel extends JPanel {

    private Image background;
    private Image imgStart, imgHow, imgExit;
    private JButton startBtn, howBtn, exitBtn;

    // زر الصوت (أيقونة) وبانل السلايدر
    private JButton soundBtn;
    private JPanel soundPanel;
    private JSlider volumeSlider;

    // نسب الأزرار كما كان عندك
    private double btnWidthRatio = 0.42;
    private double btnHeightRatio = 0.095;
    private double startYRatio = 0.35;
    private double howYRatio = 0.502;
    private double exitYRatio = 0.655;

    public MenuPanel(Window window) {
        // load images (تأكد أن الملفات موجودة في المسار المطابق)
        background = new ImageIcon("Assets/menu_bg.png").getImage();
        imgStart = new ImageIcon("Assets/button_1.png").getImage();
        imgHow = new ImageIcon("Assets/button_2.png").getImage();
        imgExit = new ImageIcon("Assets/button_3.png").getImage();

        setLayout(null);
        setOpaque(true);

        // ---- MAIN MENU BUTTONS ----
        startBtn = makeImageButton();
        howBtn = makeImageButton();
        exitBtn = makeImageButton();

        add(startBtn);
        add(howBtn);
        add(exitBtn);

        startBtn.addActionListener(e -> window.showModeMenu());
        howBtn.addActionListener(e -> window.showHowToPlay());
        exitBtn.addActionListener(e -> System.exit(0));

        // ----- SOUND ICON BUTTON (PNG transparent) -----
        soundBtn = createSpeakerButton("Assets/speaker.png", 44); // path + ideal size (px)
        add(soundBtn);

        // ---- SOUND PANEL (hidden by default) ----
        soundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            }
        };
        soundPanel.setOpaque(false);
        soundPanel.setLayout(new GridBagLayout());
        soundPanel.setVisible(false);
        soundPanel.setSize(76, 180);

        // vertical slider
        int initial = Math.round(Sound.SoundManager.getGlobalVolume() * 100f);
        volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, initial);
        volumeSlider.setOpaque(false);
        volumeSlider.setPreferredSize(new Dimension(44, 140));
        volumeSlider.setFocusable(false);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(false);
        volumeSlider.setPaintLabels(false);

        volumeSlider.addChangeListener(e -> {
            float v = volumeSlider.getValue() / 100f;
            Sound.SoundManager.setGlobalVolume(v);
        });

        JButton closeBtn = new JButton("✕");
        closeBtn.setFocusable(false);
        closeBtn.setOpaque(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setForeground(Color.LIGHT_GRAY);
        closeBtn.addActionListener(e -> hideSoundPanel());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        soundPanel.add(closeBtn, gbc);
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(6, 0, 6, 0);
        soundPanel.add(volumeSlider, gbc);

        add(soundPanel);

        // toggle sound panel on click
        soundBtn.addActionListener(e -> {
            boolean show = !soundPanel.isVisible();
            if (show) {
                volumeSlider.setValue(Math.round(Sound.SoundManager.getGlobalVolume() * 100f));
                soundPanel.setVisible(true);
                soundPanel.requestFocusInWindow();
            } else {
                hideSoundPanel();
            }
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        });

        // click outside to close panel
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (soundPanel.isVisible()) {
                    Point p = e.getPoint();
                    if (!soundPanel.getBounds().contains(p) && !soundBtn.getBounds().contains(p)) {
                        hideSoundPanel();
                    }
                }
            }
        });

        // re-layout on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
            }
        });

        SwingUtilities.invokeLater(this::layoutButtons);
    }

    private void hideSoundPanel() {
        soundPanel.setVisible(false);
    }

    // create image-button for speaker with hover-scale
    private JButton createSpeakerButton(String imagePath, int baseSizePx) {
        ImageIcon baseIcon = null;
        try {
            Image img = new ImageIcon(imagePath).getImage();
            Image scaled = img.getScaledInstance(baseSizePx, baseSizePx, Image.SCALE_SMOOTH);
            baseIcon = new ImageIcon(scaled);
        } catch (Exception ex) {
            // fallback: use text icon
        }

        JButton b = new JButton();
        b.setFocusable(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (baseIcon != null) {
            b.setIcon(baseIcon);
            // hover -> slightly larger icon
            final ImageIcon hoverIcon = new ImageIcon(((ImageIcon) baseIcon).getImage().getScaledInstance((int) (baseSizePx * 1.18), (int) (baseSizePx * 1.18), Image.SCALE_SMOOTH));
            ImageIcon finalBaseIcon = baseIcon;
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.setIcon(hoverIcon);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    b.setIcon(finalBaseIcon);
                }

                @Override
                public void mousePressed(MouseEvent e) { /* keep icon */ }
            });
        } else {
            // fallback textual speaker
            b.setText("\uD83D\uDD0A");
            b.setFont(new Font("Arial", Font.BOLD, 28));
            b.setForeground(new Color(0x0A1A3A));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.setForeground(new Color(30, 60, 120));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    b.setForeground(new Color(0x0A1A3A));
                }
            });
        }

        return b;
    }

    private JButton makeImageButton() {
        JButton b = new JButton();
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.CENTER);
        b.setForeground(new Color(40, 30, 20));
        b.setFont(new Font("Arial", Font.BOLD, 32));
        return b;
    }

    private void layoutButtons() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int btnW = (int) (w * btnWidthRatio);
        int btnH = (int) (h * btnHeightRatio);
        int cx = w / 2 - btnW / 2;

        int yStart = (int) (h * startYRatio);
        int yHow = (int) (h * howYRatio);
        int yExit = (int) (h * exitYRatio);

        startBtn.setBounds(cx, yStart - btnH / 2, btnW, btnH);
        howBtn.setBounds(cx, yHow - btnH / 2, btnW, btnH);
        exitBtn.setBounds(cx, yExit - btnH / 2, btnW, btnH);

        int fontSize = Math.max(14, btnH / 3);
        Font f = startBtn.getFont().deriveFont((float) fontSize);
        startBtn.setFont(f);
        howBtn.setFont(f);
        exitBtn.setFont(f);

        if (imgStart != null) HoverUtil.applyImageButtonHover(startBtn, imgStart, btnW, btnH);
        else startBtn.setIcon(new ImageIcon(imgStart.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));

        if (imgHow != null) HoverUtil.applyImageButtonHover(howBtn, imgHow, btnW, btnH);
        else howBtn.setIcon(new ImageIcon(imgHow.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));

        if (imgExit != null) HoverUtil.applyImageButtonHover(exitBtn, imgExit, btnW, btnH);
        else exitBtn.setIcon(new ImageIcon(imgExit.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));

        // place speaker icon top-right
        int pad = 16;
        int btnSize = 44;
        // if icon has different preferred size use it
        Dimension pref = soundBtn.getPreferredSize();
        if (pref != null && pref.width > 0) btnSize = Math.max(btnSize, pref.width);
        soundBtn.setBounds(w - btnSize - pad, pad, btnSize, btnSize);

        // sound panel under speaker
        int panelW = soundPanel.getWidth() > 0 ? soundPanel.getWidth() : 76;
        int panelH = soundPanel.getHeight() > 0 ? soundPanel.getHeight() : 180;
        int px = w - panelW - pad;
        int py = pad + btnSize + 8;
        soundPanel.setBounds(px, py, panelW, panelH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }

    // optional desert button style (not used for speaker)
    private void styleDesertButton(AbstractButton btn) {
        Color desertBase = new Color(0xC2A878);
        Color desertHover = new Color(0xD9C7A6);
        Color borderColor = new Color(0x7A6240);
        Color navyColor = new Color(0x0A1A3A);

        btn.setBackground(desertBase);
        btn.setForeground(navyColor);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(borderColor, 3));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(desertHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(desertBase);
            }
        });
    }
}
