import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleBarChartPanel extends JPanel {
    private Map<String, Double> data = new LinkedHashMap<>();
    private String title = "Comparison Chart";
    private String yAxisLabel = "Value";

    public SimpleBarChartPanel() {
        setBackground(Color.WHITE);
    }

    public void setChartData(Map<String, Double> data, String title, String yAxisLabel) {
        this.data = new LinkedHashMap<>(data);
        this.title = title;
        this.yAxisLabel = yAxisLabel;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (data.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 50;
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding;

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(title, padding, 25);

        g2.drawLine(padding, height - padding, width - padding, height - padding);
        g2.drawLine(padding, padding, padding, height - padding);
        g2.drawString(yAxisLabel, 10, padding - 10);

        double max = 1;
        for (double value : data.values()) {
            max = Math.max(max, value);
        }

        int barCount = data.size();
        int barWidth = Math.max(40, chartWidth / (barCount * 2));
        int gap = barWidth;
        int x = padding + 20;
        Color[] colors = {
                new Color(41, 128, 185),
                new Color(39, 174, 96),
                new Color(230, 126, 34),
                new Color(142, 68, 173)
        };

        int index = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            int barHeight = (int) ((entry.getValue() / max) * (chartHeight - 30));
            int y = height - padding - barHeight;

            g2.setColor(colors[index % colors.length]);
            g2.fillRect(x, y, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, barWidth, barHeight);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(entry.getKey(), x, height - padding + 15);
            g2.drawString(String.format("%.2f", entry.getValue()), x, y - 5);

            x += barWidth + gap;
            index++;
        }
    }
}
