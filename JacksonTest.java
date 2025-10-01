public class JacksonTest {
    public static void main(String[] args) {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            System.out.println("✅ Jackson ObjectMapper class found in classpath");
            
            // Try to instantiate it
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            System.out.println("✅ Jackson ObjectMapper instantiated successfully: " + mapper.getClass().getName());
            System.out.println("✅ Jackson version: " + mapper.version());
            
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Jackson ObjectMapper not found: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.out.println("❌ Error instantiating ObjectMapper: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}