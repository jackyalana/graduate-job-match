package com.lanrecruitment.service;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeAvatar(MultipartFile file);

    String storeResumeAttachment(MultipartFile file);

    Path resolveAvatarPath(String fileUrl);

    Path resolveResumeAttachmentPath(String fileUrl);
}
