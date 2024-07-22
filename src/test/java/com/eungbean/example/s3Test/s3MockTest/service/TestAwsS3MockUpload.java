package com.eungbean.example.s3Test.s3MockTest.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.eungbean.example.s3Test.s3MockTest.config.TestAwsS3MockConfig;
import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(TestAwsS3MockConfig.class)
public class TestAwsS3MockUpload {

    @Autowired
    private S3Mock s3Mock;

    @Autowired
    private AmazonS3 amazonS3;
    private MockMultipartFile mockMultipartFile;
    private final String bucketName = "test-bucket";

    @BeforeEach
    public void startS3Mock() {
        // 임시로 파일 생성
        getMockMultiPartFile();

        s3Mock.start();
    }

    @AfterAll
    public static void stopS3Mock(@Autowired S3Mock s3Mock) {
        s3Mock.stop();
    }

    @Test
    @DisplayName("AWS S3 Upload File 테스트")
    public void UploadFile() throws IOException {
        // 버킷 생성
        createBucket();

        // 임시로 파일 생성
        getMockMultiPartFile();

        // 임시 키 생성
        String key = generateKey(mockMultipartFile.getName());
        System.out.println("key: " + key);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(mockMultipartFile.getContentType());
        objectMetadata.setContentLength(mockMultipartFile.getSize());
        // 파일 업로드
        amazonS3.putObject(new PutObjectRequest(bucketName, key, mockMultipartFile.getInputStream(), objectMetadata));
        System.out.println("mockMultipartFile: " + mockMultipartFile);
        // 파일 존재 확인
        assertNotNull(amazonS3.getObject(bucketName, key));
        assertEquals(amazonS3.getObject(bucketName, key).getObjectMetadata().getContentLength(), 12);

        // 파일 다운로드
        MultiPartDownloadFile(key);
    }

    @Test
    @DisplayName("AWS S3 Download File 테스트")
    public void MultiPartDownloadFile(String key) throws IOException {
        // 버킷 생성
        createBucket();
        // 파일 가져오기
        S3Object s3Object = amazonS3.getObject(bucketName, key);
        // 파일 여부 확인
        assertNotNull(s3Object);
        assertEquals(s3Object.getObjectMetadata().getContentType(), mockMultipartFile.getContentType());

        // 파일 다운로드
        S3ObjectInputStream objectContent = s3Object.getObjectContent();
        // 파일 확장자 가져오기
        String extenstion = getFileExtension(mockMultipartFile.getOriginalFilename());

        File downloadFile = new File(key + "." + extenstion);
        saveToFile(objectContent, downloadFile);

        assertNotNull(downloadFile);

        // 파일 내용 확인
        String content = Files.readString(downloadFile.toPath());
        System.out.println("content: " + content);
        assertEquals("test_content", content);
    }

    private void getMockMultiPartFile() {
        mockMultipartFile = new MockMultipartFile(
                "img01",
                "img01.png",
                "img/png",
                "test_content".getBytes());
    }

    private void createBucket() {
        amazonS3.createBucket(bucketName);
    }

    private String generateKey(String fileName) {
        return UUID.randomUUID() + "_" + fileName;
    }
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            // 파일 이름이 null이거나 확장자가 없는 경우 빈 문자열 반환
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private void saveToFile(S3ObjectInputStream objectContent, File downloadFile) {
        // 파일 저장
        try (FileOutputStream fileOutputStream = new FileOutputStream(downloadFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = objectContent.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            //TODO : 예외 처리
            e.printStackTrace();
        }
    }

}


