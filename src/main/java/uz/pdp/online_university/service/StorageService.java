package uz.pdp.online_university.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file);
    Resource loadAsResource(String filename);
    void delete(String filename);
}
