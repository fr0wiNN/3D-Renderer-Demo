import org.yaml.snakeyaml.Yaml;
import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            case "PINK": return Color.PINK;
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
}
