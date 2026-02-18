package com.lanrecruitment.service.impl;

import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.service.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;
    private static final long RESUME_MAX_SIZE = 10 * 1024 * 1024L;

    private final String storageBaseDir;

    public FileStorageServiceImpl(@Value("${storage.base-dir:uploads}") String storageBaseDir) {
        this.storageBaseDir = storageBaseDir;
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        return store(file, "avatar", new String[]{"jpg", "jpeg", "png"}, AVATAR_MAX_SIZE);
    }

    @Override
    public String storeResumeAttachment(MultipartFile file) {
        return store(file, "resume", new String[]{"pdf", "jpg", "jpeg", "png"}, RESUME_MAX_SIZE);
    }

    private String store(MultipartFile file, String subDir, String[] allowedExt, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > maxSize) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = ext(original);
        if (!isAllowed(ext, allowedExt)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(storageBaseDir).toAbsolutePath().normalize().resolve(subDir);
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_SAVE_FAILED);
        }

        return "/uploads/" + subDir + "/" + filename;
    }

    private boolean isAllowed(String ext, String[] allowed) {
        for (String a : allowed) {
            if (a.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    private String ext(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
