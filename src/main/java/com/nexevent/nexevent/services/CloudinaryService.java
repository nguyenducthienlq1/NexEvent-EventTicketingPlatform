package com.nexevent.nexevent.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file, String folderName) {
        try {
            String publicValue = UUID.randomUUID().toString();
            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicValue,
                    "folder", folderName
            );
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("[CLOUDINARY] Lỗi upload ảnh: ", e);
            throw new RuntimeException("Không thể upload hình ảnh. Vui lòng thử lại sau!");
        }
    }

    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        String publicId = extractPublicId(imageUrl);
        if (publicId != null) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("[CLOUDINARY] Đã xóa ảnh thành công: {}", publicId);
            } catch (IOException e) {
                log.error("[CLOUDINARY] Lỗi khi xóa ảnh {}: ", imageUrl, e);
            }
        }
    }

    private String extractPublicId(String imageUrl) {
        try {
            String[] parts = imageUrl.split("/");

            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex == -1) return null;

            int startIndex = uploadIndex + 1;
            if (parts[startIndex].matches("v\\d+")) {
                startIndex++;
            }

            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = startIndex; i < parts.length; i++) {
                publicIdBuilder.append(parts[i]);
                if (i < parts.length - 1) {
                    publicIdBuilder.append("/");
                }
            }

            String publicIdWithExtension = publicIdBuilder.toString();
            return publicIdWithExtension.substring(0, publicIdWithExtension.lastIndexOf('.'));

        } catch (Exception e) {
            log.error("[CLOUDINARY] Lỗi khi phân tích URL để lấy public_id: {}", imageUrl, e);
            return null;
        }
    }
}