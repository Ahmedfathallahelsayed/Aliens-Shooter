import javax.swing.*;
import java.awt.*;

class Window extends JFrame {

    CardLayout card = new CardLayout();
    JPanel screens = new JPanel(card);

    MenuPanel mainMenu;
    ModeMenu modeMenu;
    HowToPlay howToPlay;
    GamePanel gamePanel;

    public Window() {
        setTitle("Aliens_shooter Game");
        setSize(1000, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            Sound.SoundManager.playBackground("Assets/menu_bgm.wav");
            Sound.SoundManager.setGlobalVolume(0.5f);
        } catch (Throwable t) {
            System.err.println("Sound init/play failed: " + t.getMessage());
        }

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

    public void showMenu() {
        card.show(screens, "menu");
    }

    public void showModeMenu() {
        card.show(screens, "mode");
    }

    public void showHowToPlay() {
        card.show(screens, "how");
    }

    public void startGame() {
        card.show(screens, "game");
        gamePanel.startGame();
    }



}
