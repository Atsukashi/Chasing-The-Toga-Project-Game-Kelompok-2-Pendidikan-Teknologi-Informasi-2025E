import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

public class Player {
    // --- 1. DEKLARASI POSISI DAN UKURAN KARAKTER ---
    private int x, y; // Koordinat posisi karakter di layar
    private int width = 64;  // Lebar target hitbox di layar
    private int height = 128; // Tinggi target hitbox di layar
    private int speed = 5; // Kecepatan gerak karakter

    // --- 2. DEKLARASI VARIABEL UNTUK MENYIMPAN GAMBAR (ASET) ---
    private BufferedImage idleRightImage; // Gambar diam hadap kanan
    private BufferedImage idleLeftImage;  // Gambar diam hadap kiri
    private BufferedImage[] walkRight; // Array jalan kanan (5 frame)
    private BufferedImage[] walkLeft; // Array jalan kiri (5 frame)
    private BufferedImage[] stunAnim; // Array pusing/stun (5 frame terpisah)

    // --- 3. STATUS KARAKTER SAAT INI ---
    private boolean isMoving = false; 
    private boolean facingRight = true; 
    private boolean isStunned = false; 
    private int stunDuration = 0; // Durasi pusing dalam hitungan frame
    
    // --- 4. PENGATUR KECEPATAN ANIMASI (FRAME RATE) ---
    private int aniTick = 0; // Stopwatch internal untuk ganti frame gambar
    private int aniIndex = 0; // Index urutan gambar yang sedang ditampilkan
    private int aniSpeed = 10; // Kecepatan ganti frame (makin kecil makin cepat)

    // --- 5. CONSTRUCTOR ---
    public Player(int startX, int startY) {
        this.x = startX; 
        this.y = startY; 
        loadImages(); // Otomatis load semua aset saat player dibuat
    }

    // --- 6. METHOD UNTUK MEMUAT GAMBAR DARI FOLDER ASSETS ---
    private void loadImages() {
        try { 
            // A. Memuat gambar idle (diam)
            idleRightImage = toBufferedImage(new ImageIcon(getClass().getResource("/assets/player_idle_custom.png")).getImage());
            idleLeftImage = toBufferedImage(new ImageIcon(getClass().getResource("/assets/player_idle_left.png")).getImage());

            // B. Memuat 5 gambar jalan ke kanan (walk1.png - walk5.png)
            walkRight = new BufferedImage[5]; 
            for (int i = 0; i < 5; i++) {
                String path = "/assets/walk" + (i + 1) + ".png"; 
                walkRight[i] = toBufferedImage(new ImageIcon(getClass().getResource(path)).getImage());
            }

            // C. Memuat 5 gambar jalan ke kiri (walkLeft1.png - walkLeft5.png)
            walkLeft = new BufferedImage[5]; 
            for (int i = 0; i < 5; i++) {
                String path = "/assets/walkLeft" + (i + 1) + ".png"; 
                walkLeft[i] = toBufferedImage(new ImageIcon(getClass().getResource(path)).getImage());
            }

            // D. Memuat 5 gambar pusing/stun terpisah (stunned1.png - stunned5.png)
            stunAnim = new BufferedImage[5];
            for (int i = 0; i < 5; i++) {
                String path = "/assets/stunned" + (i + 1) + ".png";
                java.net.URL url = getClass().getResource(path);
                
                if (url != null) {
                    stunAnim[i] = toBufferedImage(new ImageIcon(url).getImage());
                } else {
                    System.err.println("Gagal memuat file: " + path);
                }
            }

        } catch (Exception e) { 
            System.err.println("Terjadi kesalahan fatal saat memuat aset player! Cek folder assets.");
            e.printStackTrace(); 
        }
    }

    // --- 7. METHOD BANTUAN UNTUK MENGUBAH FORMAT GAMBAR ---
    private BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) return (BufferedImage) img;
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bimage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bimage;
    }

    // --- 8. METHOD UNTUK MENGATUR PERGANTIAN FRAME ANIMASI ---
    private void updateAnimation() {
        aniTick++; 
        if (aniTick >= aniSpeed) {
            aniTick = 0; 
            aniIndex++; 
            
            if (isStunned) {
                if (aniIndex >= 5) aniIndex = 0; // Loop animasi pusing di frame ke-5
            } else if (isMoving) {
                if (aniIndex >= 5) aniIndex = 0; // Loop animasi jalan di frame ke-5
            } else {
                aniIndex = 0; 
            }
        }
    }

    // --- 9. METHOD UNTUK UPDATE STATUS GERAKAN ---
    public void setMoving(boolean moving, boolean facingRight) {
        if (!isStunned) { // Gerakan dikunci/tidak bisa diubah kalau lagi pusing
            this.isMoving = moving;
            this.facingRight = facingRight;
        }
    }

    // --- 10. METHOD UNTUK JALAN KE KIRI ---
    public void moveLeft() {
        if (!isStunned && x > 0) { 
            x -= speed; 
            setMoving(true, false); 
        }
    }

    // --- 11. METHOD UNTUK JALAN KE KANAN ---
    public void moveRight(int screenWidth) {
        if (!isStunned && x < screenWidth - width) { 
            x += speed; 
            setMoving(true, true); 
        }
    }

    // --- 12. METHOD YANG DIPANGGIL SAAT KENA BANTAL (STUNNED) ---
    public void applyStun() {
        isStunned = true; 
        isMoving = false; 
        stunDuration = 120; // Pusing selama 120 frame (~2 detik pada 60 FPS)
        aniIndex = 0; 
    }

    // --- 13. METHOD PENGURANG WAKTU PUSING ---
    private void updateStun() {
        if (isStunned) {
            stunDuration--; 
            if (stunDuration <= 0) { 
                isStunned = false; // Efek pusing habis, player bisa gerak lagi
            }
        }
    }

    // --- 14. METHOD UTAMA UNTUK MENGGAMBAR PLAYER (FIXED ANTI-PEYANG) ---
    public void draw(Graphics g) {
        updateStun(); 
        updateAnimation(); 
        
        BufferedImage currentImage; 

        // Penentuan gambar diam (Idle) sesuai arah hadap terakhir
        if (facingRight) {
            currentImage = idleRightImage;
        } else {
            currentImage = idleLeftImage;
        }

        // Ditimpa gambar animasi gerak/stun jika kondisinya terpenuhi
        if (isStunned) {
            if (stunAnim != null && aniIndex < stunAnim.length) {
                currentImage = stunAnim[aniIndex];
            }
        } else if (isMoving) {
            if (facingRight && aniIndex < walkRight.length) {
                currentImage = walkRight[aniIndex];
            } else if (!facingRight && aniIndex < walkLeft.length) {
                currentImage = walkLeft[aniIndex];
            }
        }

        // --- PROSES RENDER AKTIF (ANTI-PEYANG & BERPIJAK PAS) ---
        if (currentImage != null) {
            int imgWidth = currentImage.getWidth();
            int imgHeight = currentImage.getHeight();

            // 1. Ambil skala default berdasarkan tinggi hitbox target (128)
            double scale = (double) this.height / imgHeight;

            // 2. Antisipasi jika frame animasi sangat lebar, agar tidak melebar keluar hitbox kiri-kanan
            if (imgWidth * scale > this.width) {
                scale = (double) this.width / imgWidth;
            }

            // 3. Hitung lebar & tinggi gambar baru yang 100% PROPORSIONAL mengikuti file asli
            int drawWidth = (int) (imgWidth * scale);
            int drawHeight = (int) (imgHeight * scale);

            // 4. Posisikan koordinat X pas di tengah-tengah kotak hitbox
            int drawX = x + (this.width - drawWidth) / 2;
            
            // 5. JANGKAR BAWAH: Memastikan bagian bawah gambar nempel di bagian bawah hitbox,
            // sehingga saat pose membungkuk/jongkok, kakinya tidak melayang.
            int drawY = y + this.height - drawHeight; 

            Graphics2D g2d = (Graphics2D) g;
            // FITUR ANTI-BLUR: Membuat visual pixel art tetap tajam kotak-kotak saat diperbesar
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            // 6. Gambar aset ke layar
            g.drawImage(currentImage, drawX, drawY, drawWidth, drawHeight, null);

        } else {
            // Fallback warna abu-abu polos jika ada gambar yang gagal diload
            g.setColor(java.awt.Color.GRAY);
            g.fillRect(x, y, width, height); 
        }
    }

    // --- 15. METHOD UNTUK HITBOX (KOTAK TABRAKAN) ---
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    
    // --- 16. METHOD GETTER ---
    public boolean isStunned() { return isStunned; } 
    public boolean facingRight() { return facingRight; } 
}