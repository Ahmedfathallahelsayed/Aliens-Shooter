import com.sun.opengl.util.Animator;
import javax.media.opengl.GLCanvas;

import javax.swing.*;
import java.awt.*;


public class Main {
    public static void main(String[] args) {
        new Game();
    }
}

class Game {

    Animator animator;
    GLCanvas glcanvas;
    MyFrame_GLEventListener listener = new MyFrame_GLEventListener();
    JButton exit = new JButton("Exit");
    JFrame frame;

    public Game() {
        frame = new JFrame("Nour Eldin Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout());
        panel1.add(exit);

        exit.setFocusable(false);
        exit.addActionListener(e -> System.exit(0));

        glcanvas = new GLCanvas();
        glcanvas.addGLEventListener(listener);
        animator = new Animator(glcanvas);

        glcanvas.addKeyListener(listener);
        glcanvas.setFocusable(true);
        glcanvas.requestFocus();

        frame.add(glcanvas, BorderLayout.CENTER);
        frame.add(panel1, BorderLayout.SOUTH);

        frame.setSize(1000, 1000);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        animator.start();
    }
}

