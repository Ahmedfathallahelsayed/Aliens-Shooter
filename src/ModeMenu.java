import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

class ModeMenu extends JPanel {

    private Image background;

    private Image imgSingle;
    private Image imgBack;

    private JButton singleBtn;
    private JButton backBtn;

    public ModeMenu(Window window) {

        background = new ImageIcon("Assets/main-bg.png").getImage();

        imgSingle = new ImageIcon("Assets/singlePlayerBTN.png").getImage();
        imgBack = new ImageIcon("Assets/Back.png").getImage();

        setLayout(null);

        singleBtn = makeImageButton();
        backBtn = makeImageButton();

        add(singleBtn);
        add(backBtn);

        singleBtn.addActionListener(e -> window.startGame());
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

        int btnW = (int) (w * 0.40);
        int btnH = (int) (h * 0.12);
        int cx = w / 2 - btnW / 2;

        singleBtn.setBounds(cx, (int) (h * 0.30), btnW, btnH);
        backBtn.setBounds(cx, (int) (h * 0.64), btnW, btnH);

        if (imgSingle != null) HoverUtil.applyImageButtonHover(singleBtn, imgSingle, btnW, btnH);
        else HoverUtil.applyTextButtonHover(singleBtn);


        if (imgBack != null) HoverUtil.applyImageButtonHover(backBtn, imgBack, btnW, btnH);
        else HoverUtil.applyTextButtonHover(backBtn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
    }
}
