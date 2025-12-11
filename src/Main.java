import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            Sound.SoundManager.playBackground("Assets/menu_bgm.wav");
            Sound.SoundManager.setGlobalVolume(0.5f);
        } catch (Throwable t) {
            System.err.println("Sound init/play failed: " + t.getMessage());
        }

        SwingUtilities.invokeLater(Window::new);
    }
}



