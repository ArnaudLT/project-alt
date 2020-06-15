package com.arnaudlt.projectalt.application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
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
public class ResourceSharing {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceSharing.class);

    private final String mediaDirectory;

    private final long chunkByteCount;


    @Autowired
    public ResourceSharing(@Value("${application.mediaDirectory}") String mediaDirectory,
                           @Value("${application.stream.chunkByteCount}") long chunkByteCount) {

        this.mediaDirectory = mediaDirectory;
        this.chunkByteCount = chunkByteCount;
    }


    @GetMapping(value = "/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {

        if (isNotValidFilename(filename)) {
            return ResponseEntity.notFound().build();
        }

        var fileRequested = new File(mediaDirectory, filename);
        var fileRequestedCanonicalPath = fileRequested.getCanonicalPath();

        return ResponseEntity.ok()
                    .headers(httpHeaders -> httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(new FileInputStream(fileRequestedCanonicalPath)));
    }


    @GetMapping(value = "/stream")
    public ResponseEntity<ResourceRegion> stream(@RequestParam String filename, @RequestHeader HttpHeaders httpHeaders)
            throws IOException {

        if (isNotValidFilename(filename)) {
            return ResponseEntity.notFound().build();
        }

        var resource = new UrlResource("file:" + mediaDirectory + filename);
        ResourceRegion resourceRegion = resourceRegion(resource, httpHeaders);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(resource)
                                             .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resourceRegion);
    }


    private ResourceRegion resourceRegion(UrlResource resource, HttpHeaders httpHeaders) throws IOException {

        var resourceLength = resource.contentLength();
        var httpHeaderRange = httpHeaders.getRange().stream().findFirst();
        ResourceRegion resourceRegion;
        if (httpHeaderRange.isPresent()) {

            var start = httpHeaderRange.get().getRangeStart(resourceLength);
            var end = httpHeaderRange.get().getRangeEnd(resourceLength);
            var rangeLength = Math.min(chunkByteCount, end - start + 1);
            resourceRegion = new ResourceRegion(resource, start, rangeLength);
        } else {
            var rangeLength = Math.min(chunkByteCount, resourceLength);
            resourceRegion = new ResourceRegion(resource, 0, rangeLength);
        }
        return resourceRegion;
    }


    // TODO find why it works with springboot 2.0.1.RELEASE (java 8) and does not work with 2.3.0.RELEASE (java 14) !!
    @GetMapping(value = "/stream2")
    public ResponseEntity<UrlResource> stream2(@RequestParam String filename)
            throws IOException {

        if (isNotValidFilename(filename)) {
            return ResponseEntity.notFound().build();
        }

        var resource = new UrlResource("file:" + mediaDirectory + filename);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }


    private boolean isNotValidFilename(String filename) throws IOException {

        var fileRequested = new File(mediaDirectory, filename);
        var fileRequestedCanonicalPath = fileRequested.getCanonicalPath();

        var fileMediaDirectory = new File(mediaDirectory);
        var fileMediaCanonicalPath = fileMediaDirectory.getCanonicalPath();

        return !fileRequestedCanonicalPath.startsWith(fileMediaCanonicalPath);
    }


    @PostMapping(value = "/upload")
    public void upload(@RequestBody MultipartFile file) throws IOException {

        LOGGER.info("Start upload of {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        String fileToUpload = mediaDirectory + file.getOriginalFilename();
        Path destination = Paths.get(fileToUpload);
        file.transferTo(destination);

        LOGGER.info("End upload of {}", destination.getFileName());
    }


    @GetMapping(value = "/list")
    public List<String> list() throws IOException {

        return Files.walk(Paths.get(mediaDirectory))
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

}
