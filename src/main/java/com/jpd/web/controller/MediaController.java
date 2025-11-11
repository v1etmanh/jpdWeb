package com.jpd.web.controller;

import com.jpd.web.dto.ModerationResponse;
import com.jpd.web.service.ModerationClient;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final ModerationClient moderationClient;

    public MediaController(ModerationClient moderationClient) {
        this.moderationClient = moderationClient;
    }
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> uploadJpeg(@RequestBody byte[] bytes) throws Exception {
        // (tuỳ chọn) kiểm tra kích thước, v.v.
        ModerationResponse res = moderationClient.check(bytes);
        if ("BLOCKED".equalsIgnoreCase(res.getDecision())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(res);
        }
        return ResponseEntity.ok(res);
    }

    // (tuỳ chọn) nhận RAW PNG
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> uploadPng(@RequestBody byte[] bytes) throws Exception {
        ModerationResponse res = moderationClient.check(bytes);
        if ("BLOCKED".equalsIgnoreCase(res.getDecision())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(res);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(
            @RequestPart("file") @NotNull MultipartFile file
    ) throws Exception {
        // (tuỳ chọn) kiểm tra Content-Type
        String ct = file.getContentType();
//        if (ct == null || !ct.startsWith("image/")) {
//            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
//                    .body("Only image/* is allowed");
//        }

        byte[] bytes = file.getBytes();

        // Gọi moderation
        ModerationResponse res = moderationClient.check(bytes);

        if ("BLOCKED".equalsIgnoreCase(res.getDecision())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(res);
        }

        // ALLOWED -> tiếp tục lưu file vào storage/S3 tuỳ bạn
        // String url = yourStorageService.save(file);
        return ResponseEntity.ok(res);
    }
}