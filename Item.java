import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

public class Item {
    // --- 1. DEKLARASI POSISI DAN UKURAN ITEM ---
    private int x, y;
    private int width = 40;  
    private int height = 40; 
    private int speed;   // UPDATE: Sekarang tidak dikunci angka 4, tapi dinamis!

    // --- 2. VARIABEL IDENTITAS ITEM ---
    private boolean isGood;  
    private int itemType;    
    private BufferedImage itemImage; 

    // --- 3. CONSTRUCTOR (UPDATE: Menerima parameter speed dari GamePanel) ---
    public Item(int x, int y, boolean isGood, int speed) {
        this.x = x;
        this.y = y;
        this.isGood = isGood;
        this.speed = speed; // Set kecepatan jatuh sesuai tingkat kesulitan semester saat ini
        
        // Mengacak variasi model item (1 sampai 3)
        this.itemType = (int) (Math.random() * 3) + 1;
        
        loadImages(); 
    }

    // --- 4. METHOD UNTUK MEMUAT ASSET GAMBAR SECARA DINAMIS ---
    private void loadImages() {
        try {
            String path = "";
            if (isGood) {
                if (itemType == 3) {
                    path = "/assets/book1.png";
                } else {
                    path = "/assets/book" + itemType + ".png";
                }
            } else {
                path = "/assets/bantal" + itemType + ".png";
            }
            
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                itemImage = toBufferedImage(new ImageIcon(url).getImage());
            } else {
                System.err.println("File aset tidak ditemukan pada path: " + path);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat aset item!");
            e.printStackTrace();
        }
    }

    private BufferedImage toBufferedImage(java.awt.Image img) {
        if (img instanceof BufferedImage) return (BufferedImage) img;
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bimage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bimage;
    }

    // --- 6. METHOD LOGIKA PERGERAKAN JATUH ---
    public void fall() {
        y += speed; 
    }

    // --- 7. METHOD UTAMA UNTUK MENGGAMBAR ITEM (ANTI-SHRINK & ANTI-PEYANG) ---
    public void draw(Graphics g) {
        if (itemImage != null) {
            int imgWidth = itemImage.getWidth();
            int imgHeight = itemImage.getHeight();

            double scaleX = (double) this.width / imgWidth;
            double scaleY = (double) this.height / imgHeight;
            double scale = Math.min(scaleX, scaleY); 

            int drawWidth = (int) (imgWidth * scale);
            int drawHeight = (int) (imgHeight * scale);

            int drawX = x + (this.width - drawWidth) / 2;
            int drawY = y + (this.height - drawHeight) / 2;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(itemImage, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            g.setColor(isGood ? java.awt.Color.GREEN : java.awt.Color.RED);
            g.fillRect(x, y, width, height);
        }
    }

    public java.awt.Rectangle getBounds() {
        return new java.awt.Rectangle(x, y, width, height);
    }

    public boolean isGoodItem() { return isGood; }
    public int getItemType() { return itemType; } 
    public int getY() { return y; }
}