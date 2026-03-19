import javax.swing.*;
import java.awt.*;

public class MainDashboard extends JFrame {
    public MainDashboard() {
        setTitle("Simulation Dashboard");
        setSize(400, 300);
        setLayout(new GridLayout(2, 1));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        JButton singleBtn = new JButton("Single-Thread Simulator");
        singleBtn.addActionListener(e -> new DemandPagingGUI().setVisible(true));
        add(singleBtn);
        
        JButton multiBtn = new JButton("Multithread Simulator");
        multiBtn.addActionListener(e -> new MultiThreadGUI().setVisible(true));
        add(multiBtn);
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainDashboard::new);
    }
}