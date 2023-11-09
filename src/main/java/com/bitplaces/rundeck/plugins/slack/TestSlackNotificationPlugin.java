import com.bitplaces.rundeck.plugins.slack.SlackNotificationPlugin;

public class TestSlackNotificationPlugin {
    public static void main(String[] args) {
        SlackNotificationPlugin plugin = new SlackNotificationPlugin();

        // Replace with your webhook URL and message
        String webhookUrl = "https://example.com/webhook/z_kuji";
        String message = "{\"text\": \"Hello, world!\"}";

        String response = plugin.invokeSlackAPIMethod(webhookUrl, message);
        System.out.println(response);
    }
}
