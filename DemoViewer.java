import javax.swing.*;

import org.yaml.snakeyaml.constructor.SafeConstructor.ConstructUndefined;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class DemoViewer {

    private static int prevMouseX, prevMouseY;
    private static boolean dragging = false;
    private static int heading = 0; 
    private static int pitch = 0;
    private static int zoom = 0;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Demo Viewer");
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        List<Triangle> tris = ShapeLoader.loadShapeFromObj("shapes/airboat.obj");
        

        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(0, 0, getWidth(), getHeight());

                double heading_tmp = Math.toRadians(heading);                      
                //double heading = Math.toRadians(xRot % 360);
                Matrix3 headingTransform = new Matrix3(new double[] {
                    Math.cos(heading_tmp), 0, -Math.sin(heading_tmp),
                    0,                 1,                   0, 
                    Math.sin(heading_tmp), 0, Math.cos(heading_tmp)
                });
                double pitch_tmp = Math.toRadians(pitch); 
                //double pitch = Math.toRadians(yRot % 360);
                Matrix3 pitchTransform = new Matrix3(new double[] {
                    1,               0,               0,
                    0, Math.cos(pitch_tmp), Math.sin(pitch_tmp),
                    0, -Math.sin(pitch_tmp), Math.cos(pitch_tmp)
                });
                Matrix3 zoomTransform = new Matrix3(new double[] {
                    zoom/10,    0,         0,
                    0,          zoom/10,   0,
                    0,          0,         zoom/10
                });

                Matrix3 transform = headingTransform.multiply(pitchTransform).multiply(zoomTransform);


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

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

                    Vertex norm = new Vertex(
                        ab.y * ac.z - ab.z * ac.y,
                        ab.z * ac.x - ab.x * ac.z,
                        ab.x * ac.y - ab.y * ac.x
                    );

                    double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;
                    double angleCos = Math.abs(norm.z);

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
                                    img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
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

        // Add mouse listeners to manipulate heading, pitch, and zoom
        renderPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                prevMouseX = e.getX();
                prevMouseY = e.getY();
                pane.setCursor(new Cursor(Cursor.MOVE_CURSOR));
                dragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                dragging = false;
            }
        });

        renderPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int deltaX = e.getX() - prevMouseX;
                    int deltaY = e.getY() - prevMouseY;

                    heading -= deltaX;
                    pitch -= deltaY;

                    prevMouseX = e.getX();
                    prevMouseY = e.getY();

                    renderPanel.repaint();
                }
            }
        });

        renderPanel.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            zoom -= notches * 10;
            renderPanel.repaint();
        });

        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static Color getShade(Color color, double shade) {
        int red = (int) (color.getRed() * shade);
        int green = (int) (color.getGreen() * shade);
        int blue = (int) (color.getBlue() * shade);
        return new Color(red, green, blue);
    }

    public static List<Triangle> inflate(List<Triangle> tris) {
        List<Triangle> result = new ArrayList<>();
        for (Triangle t : tris) {
            Vertex m1 =
                new Vertex((t.v1.x + t.v2.x)/2, (t.v1.y + t.v2.y)/2, (t.v1.z + t.v2.z)/2);
            Vertex m2 =
                new Vertex((t.v2.x + t.v3.x)/2, (t.v2.y + t.v3.y)/2, (t.v2.z + t.v3.z)/2);
            Vertex m3 =
                new Vertex((t.v1.x + t.v3.x)/2, (t.v1.y + t.v3.y)/2, (t.v1.z + t.v3.z)/2);
            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));
            result.add(new Triangle(m1, m2, m3, t.color));
        }
        for (Triangle t : result) {
            for (Vertex v : new Vertex[] { t.v1, t.v2, t.v3 }) {
                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / Math.sqrt(30000);
                v.x /= l;
                v.y /= l;
                v.z /= l;
            }
        }
        return result;
    }
}