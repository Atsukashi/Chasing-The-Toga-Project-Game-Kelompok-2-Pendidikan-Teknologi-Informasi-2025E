import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class FloatingText {
    private int x, y;
    private String text;
    private Color color;
    private int alpha = 255; // Transparansi teks (255 = pekat, 0 = hilang)
    private int speedY = 2;  // Kecepatan melayang ke atas
    private boolean dead = false;

    public FloatingText(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    // Update posisi teks ke atas dan kurangi kepekatan warnanya
    public void update() {
        y -= speedY;   // Bikin teks naik ke atas
        alpha -= 5;    // Bikin teks pelan-pelan memudar (fade out)
        
        if (alpha <= 0) {
            alpha = 0;
            dead = true; // Tandai teks sudah mati untuk dihapus dari memory
        }
    }

    public void draw(Graphics g) {
        if (!dead) {
            // Set warna dinamis mengikuti nilai alpha (transparansi) saat ini
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(text, x, y);
        }
    }

    public boolean isDead() {
        return dead;
    }
}