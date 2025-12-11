import javax.swing.*;

// أعلى Main.java: تأكد import Sound.SoundManager; موجود
public class Main {
    public static void main(String[] args) {
        // --- تهيئة/تشغيل الصوت هنا (ضع فقط دوال موجودة في SoundManager) ---
        try {
            // لو SoundManager عندك يملك playBackground و setGlobalVolume استخدمهم
            Sound.SoundManager.playBackground("Assets/menu_bgm.wav");
            Sound.SoundManager.setGlobalVolume(0.5f);
        } catch (Throwable t) {
            System.err.println("Sound init/play failed: " + t.getMessage());
        }

        SwingUtilities.invokeLater(Window::new);
    }
}



//                    MAIN WINDOW





