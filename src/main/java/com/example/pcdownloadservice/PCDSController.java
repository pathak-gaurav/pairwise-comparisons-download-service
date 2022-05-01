package com.example.pcdownloadservice;

import com.opencsv.CSVWriter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class PCDSController {

    private RestTemplate restTemplate;
 

    public PCDSController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CrossOrigin
    @RequestMapping(value = "/result", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Object getNodeForTree(HttpServletResponse response) throws IOException {
        ResponseEntity<List<NodeModel>> responseEntity = restTemplate.exchange("http://localhost:8081/v1/tree", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<NodeModel>>() {
                });


        /** Since we want file in CSV so setting the response as text/csv
         * */
        response.setContentType("text/csv");
        /** This dateformat will be used to create a unique file name.
         * */
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=pairwise_file_" + currentDateTime + ".csv";
        response.setHeader(headerKey, headerValue);

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, headerValue);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        /** Creating new File Object with the above header value.
         * */
        File file = new File(headerValue);
        /** If file does not exist then create a new file
         * */
        if (!file.isFile()) {
            file.createNewFile();
        }

        /** Creating a new CSV write to write the data on File
         * */
        CSVWriter csvWriter = new CSVWriter(new FileWriter(file));

        List<NodeModel> nodeModelList = responseEntity.getBody();
        List<String[]> nodeModeArrayList = new ArrayList<String[]>();
        for (NodeModel nm : nodeModelList) {
            if(!nm.nodeName.equalsIgnoreCase("ROOT")){
                nodeModeArrayList.add(new String[] {nm.parentName, nm.nodeName, String.valueOf(nm.value)});
            }
        }

        csvWriter.writeAll(nodeModeArrayList);
        /** Flushing the data and closing the csvWriter.
         * */
        csvWriter.flush();
        csvWriter.close();

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));


        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
