package eventdetection.downloader;

import eventdetection.common.Article;
import eventdetection.common.Source;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by laura on 2/1/16.
 */
public class TestDownloader extends Downloader {

    @Override
    public List<Article> get() {
        List<Article> articles = new ArrayList<Article>();
        Scanner scanner = new Scanner(System.in);

        // TODO: make this better before merging
        Source source = new Source(3, "TEST_SOURCE", 1);
        String title, text, url;
        while (true) {
            System.out.print("Enter article title: ");
            title = scanner.next();

            System.out.print("Enter article text: ");
            text = scanner.next();

            System.out.print("Enter article url: ");
            url = scanner.next();

            try {
                articles.add(new Article(title, text, url, source));
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Do you want to enter another article? [Y] to continue.");
            String response = scanner.next();
            if (!response.equals("Y")) {
                break;
            }
        }

        return articles;
    }

    @Override
    public void close() throws IOException {

    }
}
