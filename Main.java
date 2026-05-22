import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        // Membuat jendela aplikasi dengan judul game
        JFrame frame = new JFrame("Kejar Toga UNESA");
        
        // Memanggil class GamePanel yang berisi otak permainan + menu profil baru
        GamePanel gamePanel = new GamePanel();
        
        // Memasukkan panel game ke dalam jendela
        frame.add(gamePanel);
        
        // Pengaturan standar jendela
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(960, 540); // Resolusi game abang
        frame.setResizable(false); 
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true); 
        
        // Memulai game loop dan memutar musik utama!
        gamePanel.startGame();
    }
}