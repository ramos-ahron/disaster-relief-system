package edu.ucalgary.oop;

import edu.ucalgary.oop.api.ApiServer;
import edu.ucalgary.oop.persistence.Database;
import edu.ucalgary.oop.persistence.JdbcFamilyStore;
import edu.ucalgary.oop.persistence.RegistryLoader;
import edu.ucalgary.oop.service.ReliefRegistry;
import edu.ucalgary.oop.ui.ReliefWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Entry point.
 * <pre>
 *   java -jar app.jar        desktop app for relief workers
 *   java -jar app.jar api    family HTTP API (PORT, default 8080)
 * </pre>
 * Both read DB_URL, DB_USER and DB_PASSWORD (see Database.fromEnvironment).
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        Database database = Database.fromEnvironment();
        if (args.length > 0 && args[0].equals("api")) {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            new ApiServer(new JdbcFamilyStore(database)).start(port);
            System.out.println("API listening on port " + port + ", " + database);
            return;
        }
        ReliefRegistry registry;
        try {
            registry = RegistryLoader.load(database);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Could not load data from " + database + ":\n" + e.getMessage(),
                    "Disaster Relief Management", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> new ReliefWindow(registry).show());
    }
}
