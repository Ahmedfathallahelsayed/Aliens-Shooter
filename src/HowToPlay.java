import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class HowToPlay extends JPanel {

    private Image background;
    private Image imgBack;
    private JButton backBtn;

    private final String text =
            "Movement:\n" +
                    "  \u2191  Move Up\n" +
                    "  \u2193  Move Down\n" +
                    "  \u2190  Move Left\n" +
                    "  \u2192  Move Right\n\n" +
                    "Shoot:\n" +
                    "  SPACE";

    public HowToPlay(Window window) {
        background = new ImageIcon("Assets/main-bg.png").getImage();
        imgBack = new ImageIcon("Assets/Back.png").getImage();

        setLayout(null);
        setOpaque(true);

        // Back button (image)
        backBtn = new JButton();
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setOpaque(false);
        backBtn.addActionListener(e -> window.showMenu());
        add(backBtn);

        // layout listener
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutComponents();
            }
        });
    }

    private void layoutComponents() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // زر Back في أعلى يسار
        int btnW = (int) (w * 0.15);
        int btnH = (int) (h * 0.10);
        int btnX = 16;
        int btnY = 16;
        backBtn.setBounds(btnX, btnY, btnW, btnH);

        // اضبط أيقونة الزر
        if (imgBack != null) {
            ImageIcon ic = new ImageIcon(imgBack.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH));
            backBtn.putClientProperty("origSizeW", btnW);
            backBtn.putClientProperty("origSizeH", btnH);
            backBtn.setIcon(ic);
        }

        // فعّل تأثير hover للزر (نستخدم HoverUtil)
        if (imgBack != null) {
            HoverUtil.applyImageButtonHover(backBtn, imgBack, btnW, btnH);
        } else {
            HoverUtil.applyTextButtonHover(backBtn);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // draw background
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);

        // draw centered multiline text with arrow support
        drawCenteredMultilineText((Graphics2D) g, text, getWidth(), getHeight());
    }

    private void drawCenteredMultilineText(Graphics2D g2, String text, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // try Comic Sans MS first (user wanted Comic Sans). If it can't display arrow chars, fallback to SansSerif.
        int baseSize = Math.max(24, h / 16);
        Font font = new Font("Comic Sans MS", Font.BOLD, baseSize);
        boolean canAll = true;
        for (int cp : new int[]{'\u2191', '\u2193', '\u2190', '\u2192'}) {
            if (!font.canDisplay(cp)) {
                canAll = false;
                break;
            }
        }
        if (!canAll) {
            font = new Font("SansSerif", Font.BOLD, baseSize);
        }
        g2.setFont(font);

        String[] lines = text.split("\n");
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int totalHeight = lineHeight * lines.length;
        int startY = h / 2 - totalHeight / 2 + fm.getAscent();

        Color mainColor = new Color(59, 47, 26); // brownish
        Color shadowColor = new Color(0, 0, 0, 160);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replaceAll("  ", "  "); // keep indentation
            int lineWidth = fm.stringWidth(line);
            int x = w / 2 - lineWidth / 2;
            int y = startY + i * lineHeight;

            // shadow
            g2.setColor(shadowColor);
            g2.drawString(line, x + 2, y + 2);

            // main
            g2.setColor(mainColor);
            g2.drawString(line, x, y);
        }
    }
}
