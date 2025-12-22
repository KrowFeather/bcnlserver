package zxylearn.bcnlserver.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import zxylearn.bcnlserver.OSS.OssService;

@Tag(name = "文件模块")
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private OssService ossService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ResponseEntity<?> upload(MultipartFile file) {
        String url = ossService.uploadFile(file);
        if (url == null) {
            return ResponseEntity.status(500).body(Map.of("error", "文件上传失败"));
        }
        return ResponseEntity.ok(Map.of("url", url));
    }
}
