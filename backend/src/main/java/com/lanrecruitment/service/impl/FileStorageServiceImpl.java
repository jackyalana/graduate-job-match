package com.lanrecruitment.service.impl;

import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.service.FileStorageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024L;
    private static final long RESUME_MAX_SIZE = 5 * 1024 * 1024L;

    private static final String DEFAULT_BASE_DIR = System.getProperty("user.home") + "/lan-recruitment/uploads";

    private static final String AVATAR_URL_PREFIX = "/uploads/avatar/";
    private static final String RESUME_URL_PREFIX = "/uploads/resume/";

    private final Path baseDir;
    private final Path avatarDir;
    private final Path resumeDir;

    public FileStorageServiceImpl(Environment env) {
        Path base = Paths.get(env.getProperty("lan.storage.base-dir", DEFAULT_BASE_DIR));
        this.baseDir = base.toAbsolutePath().normalize();
        this.avatarDir = resolveDir(env.getProperty("lan.storage.avatar"), this.baseDir.resolve("avatar"));
        this.resumeDir = resolveDir(env.getProperty("lan.storage.resume"), this.baseDir.resolve("resume"));
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        return store(file, avatarDir, AVATAR_URL_PREFIX, new String[]{"jpg", "jpeg", "png"}, AVATAR_MAX_SIZE);
    }

    @Override
    public String storeResumeAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException(ErrorCode.FILE_EMPTY);
        if (file.getSize() > RESUME_MAX_SIZE) throw new BizException(ErrorCode.FILE_TOO_LARGE);
        if (!"application/pdf".equalsIgnoreCase(file.getContentType()) || !"pdf".equals(ext(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())))
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[5];
            int count = 0, n;
            while (count < header.length && (n = in.read(header, count, header.length - count)) > 0) count += n;
            if (count != 5 || !java.util.Arrays.equals(header, new byte[]{'%', 'P', 'D', 'F', '-'}))
                throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        } catch (java.io.IOException e) { throw new BizException(ErrorCode.FILE_READ_FAILED); }
        return store(file, resumeDir, RESUME_URL_PREFIX, new String[]{"pdf"}, RESUME_MAX_SIZE);
    }

    @Override
    public Path resolveAvatarPath(String fileUrl) {
        return resolveStoredPath(fileUrl, AVATAR_URL_PREFIX, avatarDir);
    }

    @Override
    public Path resolveResumeAttachmentPath(String fileUrl) {
        return resolveStoredPath(fileUrl, RESUME_URL_PREFIX, resumeDir);
    }

    private String store(MultipartFile file, Path dir, String urlPrefix, String[] allowedExt, long maxSize) {
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
        Path target = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_SAVE_FAILED);
        }

        return urlPrefix + filename;
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

    private Path resolveStoredPath(String fileUrl, String expectedPrefix, Path dir) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return null;
        }
        if (!fileUrl.startsWith(expectedPrefix)) {
            throw new BizException(ErrorCode.FILE_PATH_INVALID);
        }
        String filename = fileUrl.substring(expectedPrefix.length());
        String normalizedName = Paths.get(filename).getFileName().toString();
        if (!normalizedName.equals(filename)) {
            throw new BizException(ErrorCode.FILE_PATH_INVALID);
        }
        return dir.resolve(filename).toAbsolutePath().normalize();
    }

    private Path resolveDir(String configured, Path defaultDir) {
        if (configured == null || configured.trim().isEmpty()) {
            return defaultDir.toAbsolutePath().normalize();
        }
        Path p = Paths.get(configured);
        if (!p.isAbsolute()) {
            p = baseDir.resolve(configured);
        }
        return p.toAbsolutePath().normalize();
    }
}
