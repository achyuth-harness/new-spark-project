import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Coordinator {
    private static final ArrayList<String> moviesList = new ArrayList<>();
    private static final ArrayList<String> ratingsList = new ArrayList<>();
    private static String prodIP="";
    private static int prodPort=0;
    private static String consIP="";
    private static int consPort=0;

    public static void initialize() {
        org.jsoup.nodes.Document document = null;
        try {
            document = Jsoup.connect("http://www.imdb.com/chart/top").get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert document != null;
        for (Element row : document.select("table.chart.full-width tr")) {
            final String title = row.select(".titleColumn a").text();
            final String rating = row.select(".imdbRating").text();
            moviesList.add(title);
            ratingsList.add(rating);
        }
    }

    public static void putRequest(int i) throws IOException {
        URL url = new URL ("http://"+prodIP+":"+prodPort+"/newEntry");
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        String title, rating;
        title = moviesList.get(i); rating = ratingsList.get(i);
        String jsonInputString = "{movie= "+title+"& rating= "+rating+"}";

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
        }
    }

    public static void getRequest(String movie, String rating) throws IOException, InterruptedException {
        movie = movie.replaceAll("\\s", "%20");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://"+consIP+":"+consPort+"/updateEntry/"+movie+"/"+rating))
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length == 0){
            prodIP = StringConstants.producerHost;
            prodPort = StringConstants.producerPort;
            consIP = StringConstants.consumerHost;
            consPort = StringConstants.consumerPort;
        }
        else{
            prodIP = args[0];
            prodPort = Integer.parseInt(args[1]);
            consIP = args[2];
            consPort = Integer.parseInt(args[3]);
        }
        initialize();
        for(int i=1; i<=25; i++){
            System.out.println(i+" "+moviesList.get(i)+" "+ratingsList.get(i));
        }
        System.out.println();
        Scanner input = new Scanner(System.in);
        String confirm;
        HashSet<Integer> indexSet = new HashSet<>();
        int i;
        do {
            System.out.print("Select a movie index to be added to DB (select 0 to exit): ");
            confirm = input.nextLine();
            i = Integer.parseInt(confirm);
            if(i!=0){
                if(!indexSet.contains(i)){
                    indexSet.add(i);
                    putRequest(i);
                    TimeUnit.SECONDS.sleep(5);
                    getRequest(moviesList.get(i), ratingsList.get(i));
                }
                else
                    System.out.println("Already added this movie, select another index\n");
            }
        } while (!confirm.equals("0"));
    }
}