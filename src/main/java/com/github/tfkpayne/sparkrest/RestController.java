package com.github.tfkpayne.sparkrest;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static spark.Spark.get;

/**
 * Created by tom on 26/06/2017.
 */
public class RestController {

    private static AmazonS3 s3 = new AmazonS3Client();
    static {
        Region euCentral = Region.getRegion(Regions.EU_CENTRAL_1);
        s3.setRegion(euCentral);
    }

    private static final String BUCKET_NAME = "spark-page-bucket";
    private static final String FILE_NAME = "pages.json";


    public static void main(String[] args) {
        if (args.length > 0 && "createDB".equals(args[0])) {
            setUpDB();
        } else {
        get("/hello", (request, response) -> "Hello World");
        Optional<List<Page>> pages = readPages();
        pages.ifPresent(pageList -> {
            pageList.forEach( page -> {
                System.out.println("setting up response " + page.getContent() + " for path " + page.getPath());
                get(page.getPath(), (request, response) -> {
                            System.out.println("Getting content " + page.getContent() + " for path " + page.getPath());
                            return page.getContent();
                        }
                );
            });
        });

        System.out.println(pages);
        }

    }

    public static Optional<List<Page>> readPages() {
        S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, FILE_NAME));
        ObjectMapper mapper = new ObjectMapper();
        List<Page> pages = null;
        try {
            pages = mapper.readValue(object.getObjectContent(), new TypeReference<List<Page>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(pages);
    }

    public static void setUpDB() {
        PageDao pageDao = new PageDao("javaspark-aurora-db.cluster-cdrigdk6s55g.eu-central-1.rds.amazonaws.com", "javaspark");
        pageDao.createTables();
        readPages().ifPresent(pages -> {
            Optional<List<Page>> insertedPages = pageDao.insertPages(pages);
            if (insertedPages.isPresent()) {
                System.out.println("Inserted pages: " + insertedPages.get());
            } else {
                System.out.println("No pages were returned from query");
            }
        });

    }
}
