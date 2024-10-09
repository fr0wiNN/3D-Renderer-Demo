import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DemoViewer {

    public static double xRot = 0;
    public static double yRot = 0;
    public static double zRot = 0;

    public static void main(String[] args) {
        JFrame frame  = new JFrame("Demo Viewer");
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        //Horizonatal Slider
        //JSlider headingSlider = new JSlider(0, 360, 180);

        //Vertical Slider
        //JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        //pane.add(pitchSlider, BorderLayout.EAST);

        //JSlider zoomSlider = new JSlider(0, 200, 100);
        //JPanel bottomSlidePanel = new JPanel(new GridLayout(2,1));
        //bottomSlidePanel.add(headingSlider);
        //bottomSlidePanel.add(zoomSlider);

        //pane.add(bottomSlidePanel, BorderLayout.SOUTH);

        List<Triangle> tris = ShapeLoader.loadShapeFromYaml("shapes/cubex.yaml");

        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                                      
                double heading = Math.toRadians(xRot % 360);
                Matrix3 headingTransform = new Matrix3(new double[] {
                    Math.cos(heading), 0, -Math.sin(heading),
                    0,                 1,                   0, 
                    Math.sin(heading), 0, Math.cos(heading)
                });
                double pitch = Math.toRadians(yRot % 360);
                Matrix3 pitchTransform = new Matrix3(new double[] {
                    1,               0,               0,
                    0, Math.cos(pitch), Math.sin(pitch),
                    0, -Math.sin(pitch), Math.cos(pitch)
                });
                //double zoom = zoomSlider.getValue();
                //Matrix3 zoomTransform = new Matrix3(new double[] {
                //    zoom/100,    0,         0,
                //    0,          zoom/100,   0,
                //    0,          0,         zoom/100
                //});

                Matrix3 transform = headingTransform.multiply(pitchTransform);

                g2.setColor(Color.WHITE);

                BufferedImage img = 
                    new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] zBuffer = new double[img.getWidth() * img.getHeight()];
                // init array with very far depths
                for (int q = 0; q< zBuffer.length; q++) {
                    zBuffer[q] = Double.NEGATIVE_INFINITY;
                }

                for (Triangle t : tris) {
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);

                    //Translation
                    v1.x += getWidth() / 2;
                    v1.y += getHeight() / 2;
                    v2.x += getWidth() / 2;
                    v2.y += getHeight() / 2;
                    v3.x += getWidth() / 2;
                    v3.y += getHeight() / 2;

                    //Bounds
                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1,
                                              Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getHeight() - 1,
                                              Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea =
                        (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 = 
                                ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = 
                                ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = 
                                ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int zIndex = y * img.getWidth() + x;
                                if(zBuffer[zIndex] < depth) {
                                    img.setRGB(x, y, t.color.getRGB());
                                    zBuffer[zIndex] = depth;
                                }
                            }   
                        }
                    }
                }

                g2.drawImage(img, 0, 0, null);
            }
        };
        pane.add(renderPanel, BorderLayout.CENTER);

        //headingSlider.addChangeListener(e -> renderPanel.repaint());
        //pitchSlider.addChangeListener(e -> renderPanel.repaint());
        //zoomSlider.addChangeListener(e -> renderPanel.repaint());

        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while(true) {
            Random random = new Random();
            xRot += 0.000005 * random.nextInt(5);
            yRot += 0.000007 * random.nextInt(5);
            random = null;
            renderPanel.repaint();
        }
    }
}