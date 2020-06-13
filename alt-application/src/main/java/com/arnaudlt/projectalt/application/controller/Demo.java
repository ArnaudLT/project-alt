package com.arnaudlt.projectalt.application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class Demo {

    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

    private static final String DIRECTORY = "data\\";


    @GetMapping(value = "/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {

        File file = new File(DIRECTORY, filename);
        String fileCanonicalPath = file.getCanonicalPath();

        ResponseEntity<Resource> response;
        if (fileCanonicalPath.startsWith(DIRECTORY)) {
            response = ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(fileCanonicalPath)
                                                 .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .body(new InputStreamResource(new FileInputStream(fileCanonicalPath)));
        } else {
            response = ResponseEntity.notFound().build();
        }
        return response;
    }


    @GetMapping(value = "/stream")
    public ResponseEntity<UrlResource> stream(@RequestParam String filename) throws IOException {

        UrlResource urlResource = new UrlResource("file:" + DIRECTORY + filename);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(urlResource)
                                             .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(urlResource);
    }


    @PostMapping(value = "/upload")
    public void upload(@RequestBody MultipartFile file) throws IOException {

        LOGGER.info("Start upload of {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        String fileToUpload = DIRECTORY + file.getOriginalFilename();
        Path destination = Paths.get(fileToUpload);
        file.transferTo(destination);

        LOGGER.info("End upload of {}", destination.getFileName());
    }


    @GetMapping(value = "/list")
    public List<Path> list() throws IOException {

        return Files.walk(Paths.get(DIRECTORY))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
    }

}
