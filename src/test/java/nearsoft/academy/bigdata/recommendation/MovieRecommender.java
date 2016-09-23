package nearsoft.academy.bigdata.recommendation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by marco on 20/09/16.
 */
public class MovieRecommender {
    private static final String OUTPUT_FILEPATH = "movies.csv";
    private static final String PRODUCT_KEY = "product/productId: ";
    private static final String USER_KEY = "review/userId: ";
    private static final String SCORE_KEY = "review/score: ";

    private int totalReviews;
    private HashMap<String, Integer> users;
    private HashBiMap<String, Integer> products;
    private Recommender recommender;


    public MovieRecommender(String filePath) {
        this.users = new HashMap<String, Integer>();
        this.products = HashBiMap.create();
        File csvFile = generateCSV(filePath);
        buildRecommender(csvFile);
    }

    private File generateCSV(String filePath) {
        try {
            BufferedReader reader = getGzipReader(filePath);
            FileWriter writer = new FileWriter(OUTPUT_FILEPATH);

            String currentLine;
            String csvLine = "";
            int currentProduct = 0;

            while((currentLine = reader.readLine()) != null) {
                if (currentLine.startsWith(PRODUCT_KEY)) {
                    String productId = currentLine.substring(19);
                    if (!this.products.containsKey(productId))
                        this.products.put(productId, this.products.size());

                    currentProduct = this.products.get(productId);
                }

                else if (currentLine.startsWith(USER_KEY)) {
                    String userId = currentLine.substring(15);
                    if (!this.users.containsKey(userId))
                        this.users.put(userId, users.size());
                    this.totalReviews++;

                    csvLine = this.users.get(userId) + "," + currentProduct + ",";
                }

                else if (currentLine.startsWith(SCORE_KEY)) {
                    double score = Double.parseDouble(currentLine.substring(14));
                    csvLine += score + "\n";
                    writer.write(csvLine);
                    writer.flush();
                }
            }

            reader.close();
            writer.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return new File(OUTPUT_FILEPATH);
    }

    public void buildRecommender(File csvFile) {
        try {
            DataModel model = new FileDataModel(csvFile);
            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
            this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getRecommendationsForUser(String userId)  {
        try {
            int userIdInt = this.users.get(userId);
            List recommendations = this.recommender.recommend(userIdInt, 5);
            return getRecommendationsIds(recommendations);
        } catch (TasteException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private List<String> getRecommendationsIds(List<RecommendedItem> recommendations) {
        BiMap<Integer, String> invertedProducts = this.products.inverse();
        ArrayList<String> productIds = new ArrayList<String>();

        for (RecommendedItem recommendation : recommendations) {
            int recommendationId = (int) recommendation.getItemID();
            productIds.add(invertedProducts.get(recommendationId));
        }

        return productIds;
    }

    public int getTotalReviews() {
        return this.totalReviews;
    }

    public int getTotalProducts() {
        return this.products.size();
    }

    public int getTotalUsers() { return this.users.size(); }

    private BufferedReader getGzipReader(String filePath) throws IOException {
        InputStream fileStream = new FileInputStream(filePath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");

        return new BufferedReader(decoder);
    }
}
