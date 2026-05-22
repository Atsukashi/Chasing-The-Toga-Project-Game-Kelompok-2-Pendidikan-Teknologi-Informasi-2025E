import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;

public class SoundManager {
    
    // Objek Clip tunggal untuk mengontrol putaran musik soundtrack utama
    private static Clip bgmClip; 

    // =========================================================================
    // 1. FUNGSI MEMUTAR BGM (Murni Membuka & Memutar dari Awal)
    // =========================================================================
    public static void playBGM(String fileName) {
        try {
            // Mengambil file audio dari folder src/assets/
            InputStream audioSrc = SoundManager.class.getResourceAsStream("/assets/" + fileName);
            if (audioSrc == null) {
                System.err.println("File BGM " + fileName + " tidak ditemukan di folder assets!");
                return;
            }
            
            InputStream bufferedIn = new java.io.BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            // Mengatur agar musik otomatis melingkar (looping) tanpa henti
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
            
        } catch (Exception e) {
            System.err.println("Gagal memainkan BGM: " + fileName);
            e.printStackTrace();
        }
    }

    // =========================================================================
    // 2. FUNGSI MEMATIKAN BGM (Wajib Dipanggil untuk Reset Detik 0)
    // =========================================================================
    public static void stopBGM() {
        if (bgmClip != null) {
            if (bgmClip.isRunning()) {
                bgmClip.stop(); // Hentikan alur suara yang sedang berjalan
            }
            bgmClip.close(); // Buang instansiasi clip lama dari memori RAM
            bgmClip = null;  // Kosongkan objek agar siap membuka file baru
        }
    }

    // =========================================================================
    // 3. FUNGSI EFEK SUARA PENDEK (Sekali Bunyi: Buku +3 SKS, Stun Bantal)
    // =========================================================================
    public static void playSound(String fileName) {
        try {
            InputStream audioSrc = SoundManager.class.getResourceAsStream("/assets/" + fileName);
            if (audioSrc == null) {
                System.err.println("File efek suara " + fileName + " tidak ditemukan di folder assets!");
                return;
            }
            
            InputStream bufferedIn = new java.io.BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            // Menggunakan clip lokal terpisah agar suara efek tidak merusak alur BGM utama
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            
        } catch (Exception e) {
            System.err.println("Gagal memainkan efek suara: " + fileName);
        }
    }
}