<<<<<<< HEAD
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    IO.println(String.format("Hello and welcome!"));

    for (int i = 1; i <= 5; i++) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        IO.println("i = " + i);
    }
}
git status
git remote -v
=======
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
>>>>>>> 394f5e75b3d8c64a14e6abb1594baf9478c1be7a
