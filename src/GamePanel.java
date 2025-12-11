import com.sun.opengl.util.Animator;

import javax.media.opengl.GLCanvas;
import javax.swing.*;
import java.awt.*;

class GamePanel extends JPanel {

    GLCanvas glcanvas;
    Animator animator;
    MyFrame_GLEventListener listener;
    Window parent;

    public GamePanel(Window parent) {
        this.parent = parent;

        setLayout(new BorderLayout());

        listener = new MyFrame_GLEventListener();
        listener.setGamePanel(this);   // ربطه بالليسنر

        glcanvas = new GLCanvas();
        glcanvas.addGLEventListener(listener);
        glcanvas.addKeyListener(listener);
        glcanvas.setFocusable(true);

        animator = new Animator(glcanvas);

        add(glcanvas, BorderLayout.CENTER);
    }

    // Start a fresh game (from main menu -> start)
    public void startGame() {
        // ensure listener is not paused and game state reset for a fresh start
        listener.paused = false;

        // reset the game state to defaults so the player/aliens appear
        listener.resetGame(); // <-- مهم: يعيد تهيئة المتغيرات والـ timers

        // request focus then start animator
        SwingUtilities.invokeLater(() -> {
            glcanvas.requestFocusInWindow();
            if (!animator.isAnimating()) animator.start();
        });
    }

    public void stopGame() {
        if (animator.isAnimating()) animator.stop();
    }

    // used by pause/gameOver back-to-menu buttons
    public void returnToMenu() {
        stopGame();
        // ensure listener is unpaused so background display would work if started accidentally
        listener.paused = false;
        parent.showMenu();
    }
}
