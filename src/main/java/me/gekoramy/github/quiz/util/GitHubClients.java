package me.gekoramy.github.quiz.util;

import javafx.stage.Stage;
import me.gekoramy.github.quiz.app.AppLogin;
import me.gekoramy.github.quiz.exception.NotLoggedException;
import org.eclipse.egit.github.core.client.GitHubClient;

/**
 * @author Luca Mosetti
 */
public class GitHubClients {
    private static GitHubClient client = null;

    public static GitHubClient get() throws NotLoggedException {
        return client != null ? client : (client = login());
    }

    private static GitHubClient login() throws NotLoggedException {
        AppLogin app = new AppLogin();

        try {
            app.start(
                    new Stage()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (app.getGitHubClient() == null) throw new NotLoggedException();
        return app.getGitHubClient();
    }

    public static void logout() {
        client = null;
    }

    public static void invalidate() {
        client = null;
    }
}
