package eventdetection.downloader;

import eventdetection.common.Article;
import eventdetection.common.Source;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by laura on 2/1/16.
 */
public class TestDownloader extends Downloader {

    private Source testSource;

    public TestDownloader(Connection connection) {
        String statement = "SELECT * FROM sources WHERE source_name = 'TEST_SOURCE'";
        try {
            PreparedStatement stmt = connection.prepareStatement(statement);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int sourceId = rs.getInt("id");
            double sourceReliability = rs.getDouble("reliability");
            this.testSource = new Source(sourceId, "TEST_SOURCE", sourceReliability);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    @Override
    public List<Article> get() {
        List<Article> articles = new ArrayList<Article>();
        Scanner scanner = new Scanner(new BufferedInputStream(System.in));

        // TODO: make this better before merging
        String title, text, url;
        while (true) {
            System.out.print("Enter article title: ");
            title = scanner.nextLine();

            System.out.print("Enter article text. When all text is entered, type 'COMPLETE': ");
            StringBuilder textBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                String textResult = scanner.nextLine();
                if (textResult.equals("COMPLETE")) {
                    break;
                }
                textBuilder.append(textResult);
                textBuilder.append("\n");
            }
            text = textBuilder.toString();

            System.out.print("Enter article url: ");
            url = scanner.nextLine();

            try {
                articles.add(new Article(title, text, url, testSource));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Do you want to enter another article? [Y] to continue.");
            String response = scanner.next();
            if (!response.equals("Y")) {
                break;
            }
        }

        scanner.close();

        return articles;
    }

    @Override
    public void close() throws IOException {

    }
}
