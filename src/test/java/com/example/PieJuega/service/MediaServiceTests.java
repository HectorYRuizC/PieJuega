package com.example.PieJuega.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.PieJuega.config.CloudinaryProperties;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private UserRepository userRepository;

    private MediaService service;

    @BeforeEach
    void setUp() {
        service = new MediaService(
                cloudinary,
                new CloudinaryProperties("cloud", "key", "secret"),
                userRepository
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder()
                .id(7L)
                .roles(Set.of(Role.builder().name("ROLE_USER").build()))
                .build()));
    }

    @Test
    void uploadsAValidatedImageToTheRequestedFolder() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/cloud/image/upload/avatar.jpg",
                "public_id", "profile_photos/avatar"
        ));

        var response = service.uploadImage(7L, file, "profile_photos");

        assertEquals(
                "https://res.cloudinary.com/cloud/image/upload/avatar.jpg",
                response.url()
        );
        assertEquals("profile_photos/avatar", response.publicId());
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void preventsRegularUsersFromUploadingFieldImages() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "field.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.uploadImage(7L, file, "field_images")
        );
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rejectsFilesOutsideTheImageAllowlist() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadImage(7L, file, "profile_photos")
        );
        verify(cloudinary, never()).uploader();
    }
}
