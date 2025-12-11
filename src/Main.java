import com.sun.opengl.util.Animator;
import javax.media.opengl.GLCanvas;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Window::new);
    }
}


//                    MAIN WINDOW

class Window extends JFrame {

    CardLayout card = new CardLayout();
    JPanel screens = new JPanel(card);

    MenuPanel mainMenu;
    ModeMenu modeMenu;
    HowToPlay howToPlay;
    GamePanel gamePanel;

    public Window() {
        setTitle("Mohammed Amr Game");
        setSize(1000, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        mainMenu = new MenuPanel(this);
        modeMenu = new ModeMenu(this);
        howToPlay = new HowToPlay(this);
        gamePanel = new GamePanel(this);

        screens.add(mainMenu, "menu");
        screens.add(modeMenu, "mode");
        screens.add(howToPlay, "how");
        screens.add(gamePanel, "game");

        add(screens);
        setVisible(true);
    }

    public void showMenu() { card.show(screens, "menu"); }
    public void showModeMenu() { card.show(screens, "mode"); }
    public void showHowToPlay() { card.show(screens, "how"); }

    public void startGame() {
        card.show(screens, "game");
        gamePanel.startGame();
    }
}


class MenuPanel extends JPanel {

    private Image background;
    private Image imgStart, imgHow, imgExit;
    private JButton startBtn, howBtn, exitBtn;

    // نسب مبدئية (يمكن تعديلها أو تعديلها بصريا بالسحب)
    private double btnWidthRatio = 0.42;   // عرض الزر كنسبة من عرض اللوحة
    private double btnHeightRatio = 0.095;  // ارتفاع الزر كنسبة من ارتفاع اللوحة

    private double startYRatio = 0.35;// موضع زر Start (نسبة من ارتفاع اللوحة)
    private double howYRatio   = 0.502; // موضع How To Play
    private double exitYRatio  = 0.655; // موضع Exit

    public MenuPanel(Window window) {
        background = new ImageIcon("Assets/menu_bg.png").getImage();

        imgStart = new ImageIcon("Assets/button_1.png").getImage();
        imgHow   = new ImageIcon("Assets/button_2.png").getImage();
        imgExit  = new ImageIcon("Assets/button_3.png").getImage();

        setLayout(null);

        startBtn = makeImageButton();
        howBtn   = makeImageButton();
        exitBtn  = makeImageButton();

        add(startBtn); add(howBtn); add(exitBtn);

        startBtn.addActionListener(e -> window.showModeMenu());
        howBtn.addActionListener(e -> window.showHowToPlay());
        exitBtn.addActionListener(e -> System.exit(0));

        // عند تغيير حجم اللوحة نعيد وضع الأزرار
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
            }
        });

        // تمكين السحب لتعديل الموضع بصريًا
        DragListener drag = new DragListener();
        startBtn.addMouseListener(drag); startBtn.addMouseMotionListener(drag);
        howBtn.addMouseListener(drag); howBtn.addMouseMotionListener(drag);
        exitBtn.addMouseListener(drag); exitBtn.addMouseMotionListener(drag);
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

        int btnW = (int)(w * btnWidthRatio);
        int btnH = (int)(h * btnHeightRatio);
        int cx = w/2 - btnW/2;

        // مواضع رأسية حسب النسب
        int yStart = (int)(h * startYRatio);
        int yHow   = (int)(h * howYRatio);
        int yExit  = (int)(h * exitYRatio);

        startBtn.setBounds(cx, yStart - btnH/2, btnW, btnH);
        howBtn.setBounds(cx, yHow - btnH/2, btnW, btnH);
        exitBtn.setBounds(cx, yExit - btnH/2, btnW, btnH);

        // حجم خط النص يتناسب مع حجم الزر (لو حاط نص فوق الأيقونة)
        int fontSize = Math.max(14, btnH/3);
        Font f = startBtn.getFont().deriveFont((float)fontSize);
        startBtn.setFont(f); howBtn.setFont(f); exitBtn.setFont(f);

        // طبق hover effects للأزرار — HoverUtil سيضع الأيقونات المناسبة ويهندم التكبير عند المرور بالماوس
        if (imgStart != null) HoverUtil.applyImageButtonHover(startBtn, imgStart, btnW, btnH);
        else startBtn.setIcon(new ImageIcon(imgStart.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));

        if (imgHow != null) HoverUtil.applyImageButtonHover(howBtn, imgHow, btnW, btnH);
        else howBtn.setIcon(new ImageIcon(imgHow.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));

        if (imgExit != null) HoverUtil.applyImageButtonHover(exitBtn, imgExit, btnW, btnH);
        else exitBtn.setIcon(new ImageIcon(imgExit.getScaledInstance(btnW, btnH, Image.SCALE_SMOOTH)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }

    // Listener يسمح بسحب الأزرار لتعديل أماكنهم بصريًا
    private class DragListener extends MouseAdapter {
        private Point anchor;
        private JButton target;

        @Override
        public void mousePressed(MouseEvent e) {
            target = (JButton)e.getSource();
            anchor = e.getPoint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (target == null || anchor == null) return;
            Point parentPoint = SwingUtilities.convertPoint(target, e.getPoint(), MenuPanel.this);
            int newX = parentPoint.x - anchor.x;
            int newY = parentPoint.y - anchor.y;
            // احتفظ بداخل اللوحة
            newX = Math.max(0, Math.min(newX, getWidth() - target.getWidth()));
            newY = Math.max(0, Math.min(newY, getHeight() - target.getHeight()));
            target.setLocation(newX, newY);
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (target == null) return;
            // نحسب نسب الموضع ونطبعهم عشان تنسخهم بسهولة في الكود
            double rx = (double)(target.getX() + target.getWidth()/2) / (double)getWidth();
            double ry = (double)(target.getY() + target.getHeight()/2) / (double)getHeight();
            String name = "unknown";
            if (target == startBtn) name = "start";
            else if (target == howBtn) name = "how";
            else if (target == exitBtn) name = "exit";
            System.out.printf("Button '%s' center ratios -> x: %.4f , y: %.4f%n", name, rx, ry);
            // لو حبيت تثبت النسب تلقائيًا بعد السحب، تقدر تحدث هنا المتغيرات:
            if (target == startBtn) startYRatio = (double)(target.getY() + target.getHeight()/2) / getHeight();
            if (target == howBtn)   howYRatio  = (double)(target.getY() + target.getHeight()/2) / getHeight();
            if (target == exitBtn)  exitYRatio = (double)(target.getY() + target.getHeight()/2) / getHeight();

            target = null;
            anchor = null;
        }
    }
}






class ModeMenu extends JPanel {

    private Image background;

    private Image imgSingle;
    private Image imgMulti;
    private Image imgBack;

    private JButton singleBtn;
    private JButton multiBtn;
    private JButton backBtn;

    public ModeMenu(Window window) {

        background = new ImageIcon("Assets/main-bg.png").getImage();

        imgSingle = new ImageIcon("Assets/singlePlayerBTN.png").getImage();
        imgMulti  = new ImageIcon("Assets/multiplayerBTN.png").getImage();
        imgBack   = new ImageIcon("Assets/Back.png").getImage();

        setLayout(null);

        singleBtn = makeImageButton();
        multiBtn  = makeImageButton();
        backBtn   = makeImageButton();

        add(singleBtn);
        add(multiBtn);
        add(backBtn);

        singleBtn.addActionListener(e -> window.startGame());
        multiBtn.setEnabled(false);
        backBtn.addActionListener(e -> window.showMenu());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutButtons();
            }
        });
    }

    private JButton makeImageButton() {
        JButton b = new JButton();
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        return b;
    }

    private void layoutButtons() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int btnW = (int)(w * 0.40);
        int btnH = (int)(h * 0.12);
        int cx   = w/2 - btnW/2;

        singleBtn.setBounds(cx, (int)(h*0.30), btnW, btnH);
        multiBtn.setBounds(cx, (int)(h*0.47), btnW, btnH);
        backBtn.setBounds(cx, (int)(h*0.64), btnW, btnH);

        if (imgSingle != null) HoverUtil.applyImageButtonHover(singleBtn, imgSingle, btnW, btnH);
        else HoverUtil.applyTextButtonHover(singleBtn);

        if (imgMulti != null) HoverUtil.applyImageButtonHover(multiBtn, imgMulti, btnW, btnH);
        else HoverUtil.applyTextButtonHover(multiBtn);

        if (imgBack != null) HoverUtil.applyImageButtonHover(backBtn, imgBack, btnW, btnH);
        else HoverUtil.applyTextButtonHover(backBtn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
}





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
        int btnW = (int)(w * 0.15);
        int btnH = (int)(h * 0.10);
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
        for (int cp : new int[] {'\u2191','\u2193','\u2190','\u2192'}) {
            if (!font.canDisplay(cp)) { canAll = false; break; }
        }
        if (!canAll) {
            font = new Font("SansSerif", Font.BOLD, baseSize);
        }
        g2.setFont(font);

        String[] lines = text.split("\n");
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        int totalHeight = lineHeight * lines.length;
        int startY = h/2 - totalHeight/2 + fm.getAscent();

        Color mainColor = new Color(59, 47, 26); // brownish
        Color shadowColor = new Color(0,0,0,160);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replaceAll("  ", "  "); // keep indentation
            int lineWidth = fm.stringWidth(line);
            int x = w/2 - lineWidth/2;
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







class GamePanel extends JPanel {

    GLCanvas glcanvas;
    Animator animator;
    MyFrame_GLEventListener listener;
    Window parent;

    public GamePanel(Window parent) {
        this.parent = parent;

        setLayout(new BorderLayout());

        listener = new MyFrame_GLEventListener();

        glcanvas = new GLCanvas();
        glcanvas.addGLEventListener(listener);
        glcanvas.addKeyListener(listener);
        glcanvas.setFocusable(true);

        animator = new Animator(glcanvas);

        add(glcanvas, BorderLayout.CENTER);
    }

    public void startGame() {
        glcanvas.requestFocus();
        if(!animator.isAnimating()) animator.start();
    }
}

// HoverUtil: helper لاستخدامه مع أي JButton في المشروع
class HoverUtil {

    /**
     * Hover effect خاص بالأزرار اللي أيقونتها Image (image buttons).
     * btn: الزر
     * baseImage: الصورة الأصلية (unscaled) المستخدمة للأيقونة
     * normalW/normalH: الحجم الطبيعي الذي تستخدمه للزر (pixel)
     */
    public static void applyImageButtonHover(JButton btn, Image baseImage, int normalW, int normalH) {
        if (baseImage == null || btn == null) return;

        // حفظ الأيقونات الأصلية في client properties للعودة لها
        ImageIcon normalIcon = new ImageIcon(baseImage.getScaledInstance(normalW, normalH, Image.SCALE_SMOOTH));
        btn.putClientProperty("normalIcon", normalIcon);
        btn.setIcon(normalIcon);

        // hover icon أكبر شوية
        int hoverW = (int)(normalW * 1.08);
        int hoverH = (int)(normalH * 1.08);
        ImageIcon hoverIcon = new ImageIcon(baseImage.getScaledInstance(hoverW, hoverH, Image.SCALE_SMOOTH));
        btn.putClientProperty("hoverIcon", hoverIcon);

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // نحاول نوسّع مركز الزر بحيث يظل مركزه تقريبا ثابت
                btn.setIcon((ImageIcon)btn.getClientProperty("hoverIcon"));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setIcon((ImageIcon)btn.getClientProperty("normalIcon"));
            }
        });
    }

    /**
     * Hover effect لــ text buttons: يضيف خلفية شفافة صغيرة على hover ويد اليد.
     */
    public static void applyTextButtonHover(JButton btn) {
        if (btn == null) return;
        Color normalBg = btn.getBackground();
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(255,255,255,60)); // خلفية شبه شفافة
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setOpaque(false);
                btn.setBackground(normalBg);
            }
        });
    }
}
