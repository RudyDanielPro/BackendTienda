package com.gestion.clientes.Servicios;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService() {
        // Configuramos la cuenta con los valores reales
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dcbwngdgy",
            "api_key", "931429516999313",
            "api_secret", "lt3CLGDuSKjSX9PmnRyAD5SdUw8"
        ));
    }

    // Usamos Map<?, ?> para evitar problemas de tipos genéricos
    public Map<?, ?> upload(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new IOException("Archivo vacío");
        }
        // Retorna el mapa con la información de la subida (url, secure_url, public_id, etc.)
        return cloudinary.uploader().upload(multipartFile.getBytes(), ObjectUtils.emptyMap());
    }
}