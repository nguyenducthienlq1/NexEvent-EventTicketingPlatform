package com.nexevent.nexevent.controllers;

import com.nexevent.nexevent.domains.dto.response.RestResponse;
import com.nexevent.nexevent.services.CloudinaryService;
import com.nexevent.nexevent.utils.exception.IdInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Upload File API", description = "Upload file ảnh/video lên Cloudinary")
public class FileUploadController {

    private final CloudinaryService cloudinaryService;
    public FileUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }
    @Operation(
            summary = "Upload hình ảnh",
            description = "Tải một file hình ảnh lên Cloudinary và nhận lại URL. Nếu không truyền folder, ảnh sẽ tự động lưu vào nexevent/others."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestResponse<String>> uploadFile(
            @Parameter(description = "File ảnh cần upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Tên thư mục lưu trên Cloudinary (VD: nexevent/events, nexevent/users)", example = "nexevent/events")
            @RequestParam(value = "folder", defaultValue = "nexevent/others") String folder) {

        if (file.isEmpty()) {
            throw new IdInvalidException("File tải lên không được để trống!");
        }
        String imageUrl = cloudinaryService.uploadImage(file, folder);

        RestResponse<String> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.OK.value());
        res.setMessage("Upload ảnh thành công!");
        res.setData(imageUrl);

        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "Xóa hình ảnh",
            description = "API dành cho Frontend dọn rác nếu tạo Event lỗi, hoặc dùng để xóa ảnh thủ công."
    )
    @DeleteMapping("/delete")
    public ResponseEntity<RestResponse<Object>> deleteFile(
            @Parameter(description = "Đường dẫn (URL) của ảnh trên Cloudinary cần xóa", required = true)
            @RequestParam("url") String imageUrl) {

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IdInvalidException("URL ảnh không hợp lệ!");
        }

        // Gọi Service tiến hành xóa trên Cloudinary
        cloudinaryService.deleteImageByUrl(imageUrl);

        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.OK.value());
        res.setMessage("Yêu cầu xóa ảnh đã được xử lý thành công!");

        return ResponseEntity.ok(res);
    }
}