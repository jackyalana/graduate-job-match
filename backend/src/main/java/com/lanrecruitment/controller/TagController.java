package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.TagSaveDTO;
import com.lanrecruitment.domain.entity.Tag;
import com.lanrecruitment.domain.vo.PageVO;
import com.lanrecruitment.service.TagService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/public/tag/list")
    public Response<List<Tag>> listPublic(@RequestParam(value = "tagType", required = false) String tagType) {
        return Response.ok(tagService.listPublic(tagType));
    }

    @GetMapping("/api/public/tag/search")
    public Response<List<Tag>> searchPublic(
            @RequestParam(value = "tagType", required = false) String tagType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return Response.ok(tagService.searchPublic(tagType, keyword, limit));
    }

    @GetMapping("/api/admin/tag/list")
    public Response<List<Tag>> listAdmin(
            @RequestParam(value = "tagType", required = false) String tagType,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return Response.ok(tagService.listAdmin(tagType, status));
    }

    @GetMapping("/api/admin/tag/page")
    public Response<PageVO<Tag>> listAdminPage(
            @RequestParam(value = "tagType", required = false) String tagType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return Response.ok(tagService.listAdminPage(tagType, status, keyword, page, pageSize));
    }

    @PostMapping("/api/admin/tag/save")
    public Response<Void> save(@Valid @RequestBody TagSaveDTO dto) {
        tagService.save(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/admin/tag/delete")
    public Response<Void> delete(@Valid @RequestBody IdDTO dto) {
        tagService.delete(dto);
        return Response.ok(null);
    }
}
