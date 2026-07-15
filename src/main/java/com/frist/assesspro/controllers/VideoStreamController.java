package com.frist.assesspro.controllers;


import com.frist.assesspro.config.MinioProperties;
import com.frist.assesspro.entity.Material;
import com.frist.assesspro.service.MaterialService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.InputStream;

@Controller
@RequiredArgsConstructor
public class VideoStreamController {

    private final MaterialService materialService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @GetMapping("/materials/video/{materialId}")
    public ResponseEntity<byte[]> streamVideo(
            @PathVariable Long materialId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws Exception {

        Material material = materialService.getMaterialById(materialId);
        if (material.getType() != Material.MaterialType.VIDEO_FILE) {
            return ResponseEntity.notFound().build();
        }

        String bucketName = minioProperties.getBucketName();

        var stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucketName).object(material.getObjectKey()).build()
        );
        long fileSize = stat.size();
        String contentType = material.getContentType() != null ? material.getContentType() : "video/mp4";

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            long start = 0, end = fileSize - 1;
            String rangeValue = rangeHeader.substring("bytes=".length()).split(",")[0];
            String[] parts = rangeValue.split("-");
            if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
            if (start > fileSize - 1) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize).build();
            }
            if (end >= fileSize) end = fileSize - 1;
            long contentLength = end - start + 1;

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(material.getObjectKey())
                            .offset(start).length(contentLength).build()
            );
            byte[] data = stream.readAllBytes();
            stream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
            headers.setContentLength(contentLength);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(data);
        } else {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(material.getObjectKey()).build()
            );
            byte[] data = stream.readAllBytes();
            stream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
            headers.setContentLength(fileSize);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

            return ResponseEntity.ok().headers(headers).body(data);
        }
    }
}
