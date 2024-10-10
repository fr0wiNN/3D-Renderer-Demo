import org.yaml.snakeyaml.Yaml;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ShapeLoader {

    // Helper method to convert string color to Color object
    public static Color parseColor(String color) {
        switch (color.toUpperCase()) {
            case "WHITE": return Color.WHITE;
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "YELLOW": return Color.YELLOW;
            case "BLACK": return Color.BLACK;
            case "CYAN": return Color.CYAN;
            default: throw new IllegalArgumentException("Unknown color: " + color);
        }
    }

    // Method to safely extract a double from YAML (handles Integer and Double types)
    public static double extractDouble(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();  // Convert Integer to Double
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            throw new IllegalArgumentException("Value is not a number: " + value);
        }
    }

    // Method to load triangles from YAML file
    public static List<Triangle> loadShapeFromYaml(String fileName) {
        System.out.printf("Loading %s", fileName);

        Yaml yaml = new Yaml();
        InputStream inputStream = ShapeLoader.class.getClassLoader().getResourceAsStream(fileName);

        // Parse the YAML data
        Map<String, List<Map<String, Object>>> data = yaml.load(inputStream);

        List<Triangle> triangles = new ArrayList<>();

        // Loop through each triangle entry in the YAML
        for (Map<String, Object> triangleData : data.get("triangles")) {
            List<Map<String, Object>> verticesData = (List<Map<String, Object>>) triangleData.get("vertices");

            // Create the 3 vertices, using extractDouble to handle both Integer and Double
            Vertex v1 = new Vertex(extractDouble(verticesData.get(0).get("x")),
                                   extractDouble(verticesData.get(0).get("y")),
                                   extractDouble(verticesData.get(0).get("z")));

            Vertex v2 = new Vertex(extractDouble(verticesData.get(1).get("x")),
                                   extractDouble(verticesData.get(1).get("y")),
                                   extractDouble(verticesData.get(1).get("z")));

            Vertex v3 = new Vertex(extractDouble(verticesData.get(2).get("x")),
                                   extractDouble(verticesData.get(2).get("y")),
                                   extractDouble(verticesData.get(2).get("z")));

            // Parse the color
            String colorString = (String) triangleData.get("color");
            Color color = parseColor(colorString);

            // Create the triangle and add it to the list
            triangles.add(new Triangle(v1, v2, v3, color));
        }

        return triangles;
    }

    // Method to load triangles from an OBJ file
    public static List<Triangle> loadShapeFromObj(String fileName) {
        List<Vertex> vertices = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();
        Color defaultColor = Color.WHITE; // Default color for the triangles

        try {
            InputStream inputStream = ShapeLoader.class.getClassLoader().getResourceAsStream(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("v ")) {
                    // Vertex definition: v x y z
                    String[] parts = line.split("\\s+");
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    vertices.add(new Vertex(x, y, z));

                } else if (line.startsWith("f ")) {
                    // Face definition: f v1 v2 v3 (for triangle) or f v1 v2 v3 v4 (for quad)
                    String[] parts = line.split("\\s+");

                    // OBJ face indices are 1-based, so subtract 1 to convert to 0-based indices
                    int v1Index = Integer.parseInt(parts[1]) - 1;
                    int v2Index = Integer.parseInt(parts[2]) - 1;
                    int v3Index = Integer.parseInt(parts[3]) - 1;

                    Vertex v1 = vertices.get(v1Index);
                    Vertex v2 = vertices.get(v2Index);
                    Vertex v3 = vertices.get(v3Index);

                    // Always create the first triangle (for both triangles and quads)
                    triangles.add(new Triangle(v1, v2, v3, defaultColor));

                    // If there is a fourth vertex, it is a quad, so create a second triangle
                    if (parts.length == 5) {
                        int v4Index = Integer.parseInt(parts[4]) - 1;
                        Vertex v4 = vertices.get(v4Index);
                        // Create second triangle for quad
                        triangles.add(new Triangle(v1, v3, v4, defaultColor));
                    }

                } else if (line.startsWith("s ")) {
                    // Smoothing group, skip it
                    continue;
                }
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        //for (int x = 0 ; x < triangles.size() ; x++){
        //    Random a = new Random();
        //
        //    int r = a.nextInt(2);
        //    if (r == 1)
        //        triangles.get(x).color = Color.BLACK;
        //    else 
        //        triangles.get(x).color = Color.MAGENTA;
        //}

        //triangles.get(triangles.size() - 2).color = Color.RED;

        return triangles;
    }
}
