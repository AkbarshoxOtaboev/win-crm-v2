package uz.script.wincrm.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file
    ) {

        String fileName = storageService.uploadFile(file);

        return ResponseEntity.ok(fileName);
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(
            @PathVariable String fileName
    ) {

        Resource resource = storageService.downloadFile(fileName);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        String disposition = "image".equals(mediaType.getType()) ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + fileName + "\""
                )
                .body(resource);
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> delete(
            @PathVariable String fileName
    ) {

        storageService.delete(fileName);

        return ResponseEntity.noContent().build();
    }
}