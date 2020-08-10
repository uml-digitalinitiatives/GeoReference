package ca.umanitoba.libraries.georeferencing;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DereferencingApplication {

    private static final String USAGE = "Runs the GeoNamesLookup web application.\n";

    public static void main(String[] args) {
        if (Arrays.stream(args).anyMatch(t -> t.equalsIgnoreCase("--help") || t.equals("-h"))) {
            System.out.print(USAGE);
            System.exit(0);
        } else {
            SpringApplication.run(DereferencingApplication.class, args);
        }
    }

}
