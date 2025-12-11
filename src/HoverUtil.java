import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        int hoverW = (int) (normalW * 1.08);
        int hoverH = (int) (normalH * 1.08);
        ImageIcon hoverIcon = new ImageIcon(baseImage.getScaledInstance(hoverW, hoverH, Image.SCALE_SMOOTH));
        btn.putClientProperty("hoverIcon", hoverIcon);

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // نحاول نوسّع مركز الزر بحيث يظل مركزه تقريبا ثابت
                btn.setIcon((ImageIcon) btn.getClientProperty("hoverIcon"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setIcon((ImageIcon) btn.getClientProperty("normalIcon"));
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

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(255, 255, 255, 60)); // خلفية شبه شفافة
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setBackground(normalBg);
            }
        });
    }
}
