package uz.pdp.online_university.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentVirusScanner {

    /**
     * Mock virus scan. Returns true if safe.
     * In a real implementation, this would actually invoke ClamAV or similar.
     */
    public boolean isSafe(MultipartFile file) {
        // Here we could check magic numbers, calling external API, etc.
        // For now, it will always return true.
        return true;
    }
}
