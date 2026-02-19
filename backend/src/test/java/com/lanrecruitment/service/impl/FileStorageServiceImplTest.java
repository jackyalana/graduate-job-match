package com.lanrecruitment.service.impl;

import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAndResolveAvatarAndResumeUnderConfiguredDirs() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("lan.storage.base-dir", tempDir.toString())
                .withProperty("lan.storage.avatar", "avatar-dir")
                .withProperty("lan.storage.resume", "resume-dir");
        FileStorageServiceImpl storage = new FileStorageServiceImpl(env);

        String avatarUrl = storage.storeAvatar(new MockMultipartFile(
                "file",
                "a.png",
                "image/png",
                new byte[]{1, 2, 3}
        ));
        assertThat(avatarUrl).startsWith("/uploads/avatar/");
        Path avatarPath = storage.resolveAvatarPath(avatarUrl);
        assertThat(avatarPath).exists();
        assertThat(avatarPath.getParent()).isEqualTo(tempDir.resolve("avatar-dir").toAbsolutePath().normalize());

        String resumeUrl = storage.storeResumeAttachment(new MockMultipartFile(
                "file",
                "r.pdf",
                "application/pdf",
                new byte[]{4, 5, 6}
        ));
        assertThat(resumeUrl).startsWith("/uploads/resume/");
        Path resumePath = storage.resolveResumeAttachmentPath(resumeUrl);
        assertThat(resumePath).exists();
        assertThat(resumePath.getParent()).isEqualTo(tempDir.resolve("resume-dir").toAbsolutePath().normalize());
    }

    @Test
    void shouldRejectInvalidResumeUrlPrefix() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("lan.storage.base-dir", tempDir.toString());
        FileStorageServiceImpl storage = new FileStorageServiceImpl(env);

        assertThatThrownBy(() -> storage.resolveResumeAttachmentPath("/uploads/avatar/xx.pdf"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_PATH_INVALID);
    }
}

