package com.example.PieJuega.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.PieJuega.config.CloudinaryProperties;
import com.example.PieJuega.dto.response.ImageUploadResponseDTO;
import com.example.PieJuega.exception.MediaStorageException;
import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_IMAGE_BYTES = 8L * 1024L * 1024L;
    private static final Pattern CHAT_FOLDER = Pattern.compile("chat_images/\\d+");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "heic", "heif"
    );

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    private final UserRepository userRepository;

    public ImageUploadResponseDTO uploadImage(
            Long userId,
            MultipartFile file,
        String requestedFolder
    ) {
        if (!properties.isConfigured()) {
            throw new MediaStorageException("El almacenamiento de imágenes no está configurado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String folder = validateFolder(requestedFolder, user);
        validateFile(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "allowed_formats", ALLOWED_EXTENSIONS,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );
            String url = value(result, "secure_url");
            String publicId = value(result, "public_id");
            if (url == null || publicId == null) {
                throw new MediaStorageException("Cloudinary no devolvió una imagen válida");
            }
            return new ImageUploadResponseDTO(url, publicId);
        } catch (IOException exception) {
            throw new MediaStorageException("No pudimos guardar la imagen", exception);
        }
    }

    private String validateFolder(String requestedFolder, User user) {
        String folder = requestedFolder == null ? "" : requestedFolder.trim();
        boolean allowed = folder.equals("profile_photos")
                || folder.equals("team_shields")
                || CHAT_FOLDER.matcher(folder).matches();

        if (folder.equals("field_images")) {
            if (!isAdmin(user)) {
                throw new AccessDeniedException("Solo administración puede subir imágenes de canchas");
            }
            allowed = true;
        }
        if (!allowed) {
            throw new IllegalArgumentException("La carpeta de imágenes no es válida");
        }
        return folder;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 8 MB");
        }
        String filename = file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato de imagen no permitido");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private String value(Map<?, ?> result, String key) {
        Object value = result.get(key);
        return value == null ? null : value.toString();
    }
}
