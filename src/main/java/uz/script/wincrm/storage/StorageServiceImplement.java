package uz.script.wincrm.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uz.script.wincrm.exceptions.BadRequestException;
import uz.script.wincrm.exceptions.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImplement implements StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String uploadFile(MultipartFile file) {

        try {

            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String originalFilename =
                    StringUtils.cleanPath(file.getOriginalFilename());

            String extension = "";

            int index = originalFilename.lastIndexOf(".");

            if (index > 0) {
                extension = originalFilename.substring(index);
            }

            String generatedFileName =
                    UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(generatedFileName).normalize();
            ensureUnderUploadRoot(uploadPath, filePath);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return generatedFileName;

        } catch (IOException e) {
            throw new RuntimeException("File upload failed");
        }
    }

    @Override
    public Resource downloadFile(String fileName) {

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = resolveSafePath(uploadPath, fileName);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new ResourceNotFoundException("File not found");
            }

            return resource;

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("File download failed");
        }
    }

    @Override
    public void delete(String fileName) {

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = resolveSafePath(uploadPath, fileName);

            Files.deleteIfExists(filePath);

        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("File delete failed");
        }
    }

    private Path resolveSafePath(Path uploadRoot, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name is required");
        }

        String cleaned = StringUtils.cleanPath(fileName);
        if (cleaned.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }

        Path filePath = uploadRoot.resolve(cleaned).normalize();
        ensureUnderUploadRoot(uploadRoot, filePath);
        return filePath;
    }

    private void ensureUnderUploadRoot(Path uploadRoot, Path filePath) {
        if (!filePath.startsWith(uploadRoot)) {
            throw new BadRequestException("Invalid file path");
        }
    }
}
