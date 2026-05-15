package com.frist.assesspro.controllers.export;

import com.frist.assesspro.service.export.AsyncPdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class PdfDownloadController {

    private final AsyncPdfExportService asyncPdfExportService;

    @GetMapping("/download/{requestId}")
    public ResponseEntity<byte[]> download(@PathVariable String requestId) {
        byte[] pdf = asyncPdfExportService.getPdf(requestId);
        if (pdf == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.pdf").build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}