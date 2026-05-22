import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    // --- 1. DEKLARASI OBJEK UTAMA ---
    private Player player;
    private ArrayList<Item> items;
    private ArrayList<FloatingText> floatingTexts; 
    private Timer timer;
    private Random rand;
    
    // --- DATA ASSET GAMBAR ---
    private Image backgroundImage;   // Digunakan saat in-game (Layar Main)
    private Image menuBackgroundImage; // UPDATE: Menampung gambar background_menu.png abang
    private Image uiTitle;
    private Image uiBoardWin;
    private Image uiBoardLose;
    private Image uiHudSksTime; 
    private Image hudStatusMHS;
    
    // --- 2. STATUS GAME ---
    // -1 = Menu Manajemen Profil (Add/Delete/Select)
    // 0 = Menu Utama, 1 = Sedang Main, 2 = Menang Lulus, 3 = Kalah Ngulang
    private int gameState = -1; 
    
    // --- DATA STRUKTUR PROFIL & TIME ATTACK INTERNAL ---
    private ArrayList<String> daftarUsername = new ArrayList<>();
    private ArrayList<Integer> daftarHighScore = new ArrayList<>(); // Rekor waktu tercepat (detik)
    private int indexProfilAktif = -1; 
    private int indexPilihanMenu = 0;   

    // --- INTEGRASI FITUR: SISTEM WINDOW SCROLL PROFIL ---
    private int scrollOffset = 0;          
    private final int MAKS_VIEW_PROFIL = 6; 

    // --- 3. DATA HUD (STATISTIK) ---
    private int skorSKS = 0;
    private int targetSKS = 144; // Target lulus standar UNESA
    private int waktuSisa = 60;
    private int waktuDihabiskan = 0;
    private int frameCount = 0;

    // --- 4. BENDERA STATUS TOMBOL ---
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // --- 5. CONSTRUCTOR ---
    public GamePanel() {
        daftarUsername.add("David Fidan");
        daftarHighScore.add(999);          
        daftarUsername.add("Maba UNESA");
        daftarHighScore.add(999);

        initGame(); 
        loadUIAssets(); 
        setFocusable(true); 
        addKeyListener(this); 
        timer = new Timer(16, this); 
        
        loadProfiles();
    }

    // --- MEMUAT SEMUA ASET GAMBAR LINGKUNGAN DAN UI ---
    private void loadUIAssets() {
        try {
            backgroundImage = new ImageIcon(getClass().getResource("/assets/BackgroundEnvironment.png")).getImage();
            
            // UPDATE: Memuat gambar background menu 8-bit baru ukuran 960x540
            menuBackgroundImage = new ImageIcon(getClass().getResource("/assets/background_menu.png")).getImage();
            
            uiTitle = new ImageIcon(getClass().getResource("/assets/ui_title.png")).getImage();
            uiBoardWin = new ImageIcon(getClass().getResource("/assets/ui_board_win.png")).getImage();
            uiBoardLose = new ImageIcon(getClass().getResource("/assets/ui_board_lose.png")).getImage();
            uiHudSksTime = new ImageIcon(getClass().getResource("/assets/ui_hud_sks_time.png")).getImage();
            hudStatusMHS = new ImageIcon(getClass().getResource("/assets/hud_statusMHS.png")).getImage();
        } catch (Exception e) {
            System.err.println("Gagal memuat beberapa aset gambar UI! Pastikan namanya sudah benar di folder assets.");
        }
    }

    private void initGame() {
        player = new Player(375, 350); 
        items = new ArrayList<>();
        floatingTexts = new ArrayList<>(); 
        rand = new Random();
        skorSKS = 0; 
        waktuSisa = 60; 
        waktuDihabiskan = 0;
        frameCount = 0;
        leftPressed = false;
        rightPressed = false;
    }

    public void startGame() {
        timer.start();
        SoundManager.playBGM("main_soundtrack.wav");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // =========================================================================
        // A. PROSES RENDER BACKGROUND JADI EFEK HD BOKEH / BLUR
        // =========================================================================
        // Menentukan background mana yang aktif digunakan berdasarkan gameState
        Image activeBG = (gameState == -1 || gameState == 0) ? menuBackgroundImage : backgroundImage;

        if (activeBG != null) {
            int currentWidth = getWidth();
            int currentHeight = getHeight();

            if (currentWidth > 0 && currentHeight > 0) {
                BufferedImage blurredBG = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gBG = blurredBG.createGraphics();
                gBG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                gBG.drawImage(activeBG, 0, 0, currentWidth, currentHeight, null);
                gBG.dispose();

                float[] matrix = {
                    1f/16f, 2f/16f, 1f/16f,
                    2f/16f, 4f/16f, 2f/16f,
                    1f/16f, 2f/16f, 1f/16f
                };
                BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, matrix), ConvolveOp.EDGE_NO_OP, null);
                BufferedImage finalBG = op.filter(blurredBG, null);

                g2d.drawImage(finalBG, 0, 0, null);
                
                java.awt.GradientPaint vignette = new java.awt.GradientPaint(
                    0, 0, new Color(0, 0, 0, 15), 0, currentHeight, new Color(0, 0, 0, 40)
                );
                g2d.setPaint(vignette);
                g2d.fillRect(0, 0, currentWidth, currentHeight);
            }
        }

        // =========================================================================
        // B. PROSES RENDER UI BERDASARKAN STATUS GAME
        // =========================================================================
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // --- LAYAR GAMESTATE -1: MENU MANAJEMEN PROFIL NEW GLOW LOOK ---
        if (gameState == -1) {
            g.setColor(new Color(6, 12, 24, 210)); // Opacity diturunkan dikit biar gambar mbois rektorat mengintip samar
            g.fillRect(0, 0, getWidth(), getHeight());

            int boardW = 640; 
            int boardH = 410;
            int boardX = centerX - (boardW / 2);
            int boardY = centerY - (boardH / 2) - 20;
            
            g2d.setStroke(new BasicStroke(4.0f));
            g2d.setColor(new Color(0, 100, 255, 60)); 
            g2d.drawRoundRect(boardX - 3, boardY - 3, boardW + 6, boardH + 6, 25, 25);
            g2d.setColor(new Color(0, 195, 255, 120)); 
            g2d.drawRoundRect(boardX - 1, boardY - 1, boardW + 2, boardH + 2, 22, 22);
            
            g2d.setColor(new Color(13, 22, 43, 240));
            g2d.fillRoundRect(boardX, boardY, boardW, boardH, 20, 20);
            
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.setColor(new Color(0, 195, 255));
            g2d.drawRoundRect(boardX, boardY, boardW, boardH, 20, 20);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            String textJudul = "PILIH PROFIL MAHASISWA";
            FontMetrics fmJudul = g2d.getFontMetrics();
            int judulX = centerX - (fmJudul.stringWidth(textJudul) / 2);
            
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.drawString(textJudul, judulX + 2, boardY + 47);
            g2d.setColor(new Color(255, 220, 0));
            g2d.drawString(textJudul, judulX, boardY + 45);

            g2d.setPaint(new GradientPaint(boardX + 50, 0, new Color(0, 195, 255, 0), centerX, 0, new Color(0, 195, 255, 255), true));
            g2d.fillRect(boardX + 40, boardY + 65, boardW - 80, 2);

            int startY = boardY + 95;
            int slotW = boardW - 60;
            int slotH = 34;
            int slotX = boardX + 30;
            
            int batasAtasRender = Math.min(scrollOffset + MAKS_VIEW_PROFIL, daftarUsername.size());
            for (int i = scrollOffset; i < batasAtasRender; i++) {
                int rekorWaktu = daftarHighScore.get(i);
                String txtRekor = (rekorWaktu == 999) ? "Belum Lulus" : rekorWaktu + " Detik";
                String teksProfil = daftarUsername.get(i) + "  |  [Rekor: " + txtRekor + "]";
                
                g2d.setFont(new Font("Arial", Font.BOLD, 15));
                FontMetrics fmProfil = g2d.getFontMetrics();
                int profilX = centerX - (fmProfil.stringWidth(teksProfil) / 2);
                
                if (i == indexPilihanMenu) {
                    GradientPaint gradSlot = new GradientPaint(
                        slotX, startY, new Color(0, 120, 255, 100),
                        slotX + slotW, startY, new Color(0, 195, 255, 20)
                    );
                    g2d.setPaint(gradSlot);
                    g2d.fillRoundRect(slotX, startY, slotW, slotH, 8, 8);
                    
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.setColor(new Color(255, 215, 0));
                    g2d.drawRoundRect(slotX, startY, slotW, slotH, 8, 8);
                    
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(teksProfil, profilX, startY + 22);
                    
                    if ((frameCount / 15) % 2 == 0) {
                        g2d.setColor(new Color(255, 215, 0));
                        g2d.drawString("►", slotX + 15, startY + 22);
                        g2d.drawString("◄", slotX + slotW - 25, startY + 22);
                    }
                } else {
                    g2d.setColor(new Color(20, 32, 59, 120));
                    g2d.fillRoundRect(slotX, startY, slotW, slotH, 8, 8);
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.drawRoundRect(slotX, startY, slotW, slotH, 8, 8);
                    
                    g2d.setColor(new Color(170, 185, 210));
                    g2d.drawString(teksProfil, profilX, startY + 22);
                }
                startY += 44; 
            }

            g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2d.setColor(new Color(0, 195, 255));
            if (scrollOffset > 0) {
                g2d.drawString("▲", centerX - 5, boardY + 86);
            }
            if (scrollOffset + MAKS_VIEW_PROFIL < daftarUsername.size()) {
                g2d.drawString("▼", centerX - 5, boardY + 395);
            }

            g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
            FontMetrics fmNav = g2d.getFontMetrics(); 

            String textNavigasi = "[↑/↓] Navigasi Profil  |  [ENTER] Pilih Akun";
            int navX = centerX - (fmNav.stringWidth(textNavigasi) / 2);
            g2d.setColor(new Color(150, 170, 200));
            g2d.drawString(textNavigasi, navX, getHeight() - 40);

            String textTambah = "[A] Tambah Profil  |  [DELETE] Hapus Profil  |  [ESC] Keluar Game";
            int tambahX = centerX - (fmNav.stringWidth(textTambah) / 2);
            g2d.setColor(new Color(0, 215, 255));
            g2d.drawString(textTambah, tambahX, getHeight() - 20);

        } else if (gameState == 0) { // --- 1. TAMPILAN MENU UTAMA PREMIUM ---
            g.setColor(new Color(8, 16, 36, 120)); // Diturunkan ke 120 agar rincian pixel art UNESA terlihat jernih
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // FRAME BORDER LAYAR UTAMA 
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.setColor(new Color(0, 195, 255, 80));
            g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 15, 15);
            
            if (uiTitle != null) {
                int originalW = uiTitle.getWidth(null);
                int originalH = uiTitle.getHeight(null);
                int titleW = 480; 
                int titleH = (titleW * originalH) / originalW; 
                g2d.drawImage(uiTitle, centerX - (titleW / 2), centerY - 300, titleW, titleH, null);
            } else {
                g2d.setFont(new Font("Impact", Font.ITALIC, 54)); 
                String textMenuJudul = "CHASING THE TOGA";
                FontMetrics fmMenuJudul = g2d.getFontMetrics();
                int menuJudulX = centerX - (fmMenuJudul.stringWidth(textMenuJudul) / 2);
                int menuJudulY = centerY - 60;
                
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.drawString(textMenuJudul, menuJudulX + 4, menuJudulY + 4);
                g2d.setColor(new Color(0, 120, 255, 180));
                g2d.drawString(textMenuJudul, menuJudulX + 2, menuJudulY + 2);
                g2d.setColor(new Color(255, 215, 0));
                g2d.drawString(textMenuJudul, menuJudulX, menuJudulY);
            }
            
            // CARD PANEL INFO PEJUANG AKTIF & REKOR (KIRI ATAS)
            int cardW = 270;
            int cardH = 54;
            g2d.setColor(new Color(13, 22, 43, 200));
            g2d.fillRoundRect(20, 20, cardW, cardH, 10, 10);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.setColor(new Color(0, 195, 255, 150)); 
            g2d.drawRoundRect(20, 20, cardW, cardH, 10, 10);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 13));
            g2d.setColor(new Color(180, 200, 235));
            g2d.drawString("Pejuang Aktif: ", 32, 40);
            g2d.setColor(Color.CYAN);
            g2d.drawString(daftarUsername.get(indexProfilAktif), 127, 40);
            
            int rekorAktif = daftarHighScore.get(indexProfilAktif);
            String txtRekorAktif = (rekorAktif == 999) ? "Belum Lulus" : rekorAktif + " detik";
            g2d.setColor(new Color(180, 200, 235));
            g2d.drawString("Waktu Terbaik: ", 32, 60);
            g2d.setColor(new Color(255, 220, 0));
            g2d.drawString(txtRekorAktif, 127, 60);
            
            // TEKS KEDIP MULAI AUTO CENTER
            if ((frameCount / 25) % 2 == 0) {
                g2d.setColor(new Color(255, 230, 0));
                g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
                
                String textMulai = "►► TEKAN ENTER UNTUK MULAI NGEJAR TOGA ◄◄";
                FontMetrics fmMulai = g2d.getFontMetrics();
                int mulaiX = centerX - (fmMulai.stringWidth(textMulai) / 2);
                
                g2d.drawString(textMulai, mulaiX, centerY + 65);
            }
            
            // TEKS KONTROL BAWAH AUTO CENTER (FIXED PARAMETER)
            g2d.setColor(new Color(160, 180, 210));
            g2d.setFont(new Font("Arial", Font.PLAIN, 13));
            
            String textKontrol = "Kontrol: Gunakan PANAH KIRI / KANAN untuk Bergerak | [ESC] Ganti Akun";
            FontMetrics fmKontrol = g2d.getFontMetrics();
            int kontrolX = centerX - (fmKontrol.stringWidth(textKontrol) / 2);
            
            g2d.setColor(new Color(0, 0, 0, 100));
            // FIX: Sudah memakai 6 parameter int lengkap berpasangan, anti-error!
            g2d.fillRoundRect(kontrolX - 20, centerY + 102, fmKontrol.stringWidth(textKontrol) + 40, 26, 6, 6);
            
            g2d.setColor(new Color(200, 215, 240));
            g2d.drawString(textKontrol, kontrolX, centerY + 119);
            
        } else if (gameState == 1) { // --- 2. LAYAR SEDANG BERMAIN ---
            player.draw(g);
            for (Item item : items) item.draw(g);
            for (FloatingText ft : floatingTexts) ft.draw(g);

            int panelW = 230; int panelH = 45; int padY = 12; int sksX = 15;

            if (uiHudSksTime != null) {
                Font fontHUD = new Font(Font.SERIF, Font.BOLD, 17);
                g2d.setFont(fontHUD);

                g2d.drawImage(uiHudSksTime, sksX, padY, panelW, panelH, null);
                g2d.setColor(Color.BLACK);
                g2d.drawString("SKS:", sksX + 61, padY + 29);
                
                g2d.setColor(new Color(255, 180, 0)); 
                g2d.drawString("SKS:", sksX + 60, padY + 28);
                g2d.setColor(Color.BLACK);
                g2d.drawString(skorSKS + " / " + targetSKS, sksX + 105, padY + 28);

                int timeX = getWidth() - panelW - 15;
                g2d.drawImage(uiHudSksTime, timeX, padY, panelW, panelH, null);
                g2d.setColor(Color.BLACK);
                g2d.drawString("TIME:", timeX + 71, padY + 29);
                
                g2d.setColor(new Color(255, 180, 0)); 
                g2d.drawString("TIME:", timeX + 70, padY + 28);
                g2d.setColor(Color.BLACK);
                g2d.drawString(waktuSisa + "s", timeX + 125, padY + 28);
                
            } else {
                g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
                g.drawString("SKS: " + skorSKS + " / " + targetSKS, 20, 33);
                g.drawString("WAKTU: " + waktuSisa + "s", getWidth() - panelW, 33);
            }
            
            String txtStatus = (skorSKS < 50) ? "Maba (Semester Awal)" : (skorSKS < 100) ? "Sibuk KKN / Magang" : "AWAS SKRIPSI!";
            Color colorStatus = (skorSKS < 50) ? Color.GREEN : (skorSKS < 100) ? Color.YELLOW : Color.RED;

            if (hudStatusMHS != null) {
                int statusW = 260; int statusH = 45; int statusX = centerX - (statusW / 2);
                g2d.drawImage(hudStatusMHS, statusX, padY, statusW, statusH, null);
                g2d.setFont(new Font(Font.SERIF, Font.BOLD, 13));
                
                String gabungStatus = "Status: " + txtStatus;
                FontMetrics fmStatus = g2d.getFontMetrics();
                int statusTeksX = statusX + (statusW - fmStatus.stringWidth(gabungStatus)) / 2;
                int textY = padY + 28;
                
                g2d.setColor(Color.BLACK);
                g2d.drawString(gabungStatus, statusTeksX + 1, textY + 1);
                
                String labelStatus = "Status: ";
                g2d.setColor(new Color(255, 180, 0)); 
                g2d.drawString(labelStatus, statusTeksX, textY);
                
                int nilaiStatusX = statusTeksX + fmStatus.stringWidth(labelStatus);
                g2d.setColor(colorStatus);
                g2d.drawString(txtStatus, nilaiStatusX, textY);
            }
            
        } else { // --- 3. LAYAR GAME OVER / WIN BOARD ---
            g.setColor(new Color(0, 0, 0, 180)); g.fillRect(0, 0, getWidth(), getHeight());
            int boardW = 550; int boardH = 320; int boardX = centerX - (boardW / 2); int boardY = centerY - (boardH / 2);
            
            if (gameState == 2) {
                if (uiBoardWin != null) g2d.drawImage(uiBoardWin, boardX, boardY, boardW, boardH, null);
            } else {
                if (uiBoardLose != null) g2d.drawImage(uiBoardLose, boardX, boardY, boardW, boardH, null);
            }
            
            g2d.setColor(new Color(255, 215, 0)); g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
            g2d.drawString("SCORE", centerX - 120, centerY - -5); 
            g2d.drawString("TIME", centerX + 80, centerY - -5);  
            
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Monospaced", Font.BOLD, 22));
            String txtSKS = skorSKS + " SKS";
            String txtWaktu = (gameState == 2) ? waktuDihabiskan + " SECS" : "LIMIT!";
            
            FontMetrics fm = g2d.getFontMetrics();
            int txtSKSX = (centerX - 142) - (fm.stringWidth(txtSKS) / 2);
            int txtWaktuX = (centerX + 142) - (fm.stringWidth(txtWaktu) / 2);
            
            g2d.drawString(txtSKS, txtSKSX - -50, centerY - -35);
            g2d.drawString(txtWaktu, txtWaktuX - 35, centerY - -35);
            
            if ((frameCount / 25) % 2 == 0) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
                
                String textRestart = ">> Tekan ENTER untuk Registrasi KRS Ulang <<";
                FontMetrics fmRestart = g2d.getFontMetrics();
                int restartX = centerX - (fmRestart.stringWidth(textRestart) / 2);
                
                g2d.drawString(textRestart, restartX, centerY + 200);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frameCount++; 
        if (gameState == 1) {
            if (leftPressed) player.moveLeft();
            else if (rightPressed) player.moveRight(getWidth()); 
            else player.setMoving(false, player.facingRight());
            
            if (frameCount % 60 == 0) { 
                waktuSisa--; waktuDihabiskan++;  
                if (waktuSisa <= 0) gameState = 3; 
            }

            int currentItemSpeed = (skorSKS < 50) ? 4 : (skorSKS < 100) ? 6 : 8;
            int currentSpawnInterval = (skorSKS < 50) ? 40 : (skorSKS < 100) ? 30 : 20;

            if (frameCount % currentSpawnInterval == 0) {
                items.add(new Item(rand.nextInt(getWidth() - 50), 0, rand.nextInt(100) < 65, currentItemSpeed));
            }

            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i); item.fall(); 
                if (player.getBounds().intersects(item.getBounds())) {
                    if (item.isGoodItem()) {
                        skorSKS += 3; 
                        floatingTexts.add(new FloatingText(item.getBounds().x, item.getBounds().y - 10, "+3 SKS", Color.GREEN));
                        SoundManager.playSound("getBook.wav");
                        if (skorSKS >= targetSKS) {
                            gameState = 2; // Menang Lulus
                            
                            int rekorLama = daftarHighScore.get(indexProfilAktif);
                            if (waktuDihabiskan < rekorLama) {
                                daftarHighScore.set(indexProfilAktif, waktuDihabiskan);
                                saveProfiles(); 
                            }
                        }
                    } else {
                        player.applyStun(); waktuSisa -= 2; waktuDihabiskan += 2; 
                        floatingTexts.add(new FloatingText(item.getBounds().x, item.getBounds().y - 10, "STUNNED!", Color.RED));
                        floatingTexts.add(new FloatingText(item.getBounds().x, item.getBounds().y + 12, "+2 Detik Kuliah", Color.ORANGE)); 
                        SoundManager.playSound("getBantal.wav");
                    }
                    items.remove(i); i--; 
                } else if (item.getY() > getHeight()) {
                    items.remove(i); i--;
                }
            }

            for (int i = 0; i < floatingTexts.size(); i++) {
                FloatingText ft = (FloatingText) floatingTexts.get(i); ft.update();
                if (ft.isDead()) { floatingTexts.remove(i); i--; }
            }
        }
        repaint(); 
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (gameState == -1) {
            if (key == KeyEvent.VK_DOWN) {
                if (!daftarUsername.isEmpty()) {
                    indexPilihanMenu = (indexPilihanMenu + 1) % daftarUsername.size();
                    if (indexPilihanMenu >= scrollOffset + MAKS_VIEW_PROFIL) {
                        scrollOffset = indexPilihanMenu - MAKS_VIEW_PROFIL + 1;
                    } else if (indexPilihanMenu == 0) {
                        scrollOffset = 0; 
                    }
                }
            } else if (key == KeyEvent.VK_UP) {
                if (!daftarUsername.isEmpty()) {
                    indexPilihanMenu = (indexPilihanMenu - 1 + daftarUsername.size()) % daftarUsername.size();
                    if (indexPilihanMenu < scrollOffset) {
                        scrollOffset = indexPilihanMenu;
                    } else if (indexPilihanMenu == daftarUsername.size() - 1) {
                        scrollOffset = Math.max(0, daftarUsername.size() - MAKS_VIEW_PROFIL);
                    }
                }
            } else if (key == KeyEvent.VK_ENTER && !daftarUsername.isEmpty()) {
                indexProfilAktif = indexPilihanMenu;
                gameState = 0; 
            } else if (key == KeyEvent.VK_A) {
                String namaBaru = JOptionPane.showInputDialog(this, "Masukkan Nama Profile Baru:", "Tambah Pejuang Toga", JOptionPane.PLAIN_MESSAGE);
                if (namaBaru != null && !namaBaru.trim().isEmpty()) {
                    daftarUsername.add(namaBaru.trim());
                    daftarHighScore.add(999); 
                    indexPilihanMenu = daftarUsername.size() - 1;
                    
                    if (daftarUsername.size() > MAKS_VIEW_PROFIL) {
                        scrollOffset = daftarUsername.size() - MAKS_VIEW_PROFIL;
                    }
                    saveProfiles(); 
                }
            } else if (key == KeyEvent.VK_DELETE && !daftarUsername.isEmpty()) {
                int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus profil " + daftarUsername.get(indexPilihanMenu) + "?", "Hapus Profil", JOptionPane.YES_NO_OPTION);
                if (konfirmasi == JOptionPane.YES_OPTION) {
                    daftarUsername.remove(indexPilihanMenu);
                    daftarHighScore.remove(indexPilihanMenu);
                    indexPilihanMenu = 0;
                    scrollOffset = 0; 
                    saveProfiles(); 
                }
            } else if (key == KeyEvent.VK_ESCAPE) {
                int keluar = JOptionPane.showConfirmDialog(this, "Yakin ingin keluar dari game Kejar Toga UNESA?", "Keluar Game", JOptionPane.YES_NO_OPTION);
                if (keluar == JOptionPane.YES_OPTION) {
                    saveProfiles(); 
                    System.exit(0); 
                }
            }
            
        } else if (gameState == 0) {
            if (key == KeyEvent.VK_ENTER) {
                initGame(); gameState = 1; 
                SoundManager.stopBGM();
                SoundManager.playBGM("main_soundtrack.wav");
            } else if (key == KeyEvent.VK_ESCAPE) {
                gameState = -1; 
            }
            
        } else if (gameState == 2 || gameState == 3) {
            if (key == KeyEvent.VK_ENTER) gameState = 0;
            
        } else if (gameState == 1) {
            if (key == KeyEvent.VK_LEFT) leftPressed = true;
            if (key == KeyEvent.VK_RIGHT) rightPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == 1) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) leftPressed = false;
            if (key == KeyEvent.VK_RIGHT) rightPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {} 

    private void saveProfiles() {
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter("data_pemain.txt"))) {
            for (int i = 0; i < daftarUsername.size(); i++) {
                writer.write(daftarUsername.get(i) + "," + daftarHighScore.get(i));
                writer.newLine();
            }
            System.out.println("Data profil berhasil diamankan di data_pemain.txt!");
        } catch (Exception e) {
            System.err.println("Gagal mengamankan data profil!");
        }
    }

    private void loadProfiles() {
        java.io.File file = new java.io.File("data_pemain.txt");
        if (!file.exists()) return; 
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            daftarUsername.clear();
            daftarHighScore.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 2) {
                    daftarUsername.add(data[0]);
                    daftarHighScore.add(Integer.parseInt(data[1]));
                }
            }
            System.out.println("Seluruh data pejuang toga berhasil dimuat!");
        } catch (Exception e) {
            System.err.println("Gagal memuat file simpanan!");
        }
    }
}