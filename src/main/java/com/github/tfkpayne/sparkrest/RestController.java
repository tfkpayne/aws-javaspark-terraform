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
        get("/hello", (request, response) -> "Hello World");
        S3Object object = s3.getObject(new GetObjectRequest(BUCKET_NAME, FILE_NAME));
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Page> pages = mapper.readValue(object.getObjectContent(), new TypeReference<List<Page>>(){});
            pages.forEach(page -> {
                System.out.println("setting up response " + page.getContent() + " for path " + page.getPath());
                get(page.getPath(), (request, response) -> {
                    System.out.println("Getting content " + page.getContent() + " for path " + page.getPath());
                    return page.getContent();
                });
            });

            System.out.println(pages);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
