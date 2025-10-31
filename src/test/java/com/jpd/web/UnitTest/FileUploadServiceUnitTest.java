package com.jpd.web.UnitTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import com.jpd.web.service.FileUploadService;
import com.jpd.web.service.FireBaseService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.jpd.web.exception.ApiException;
import com.jpd.web.model.Creator;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.service.utils.ValidationResources;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceUnitTest {

    @Mock
    private PendingImgRepository pendingImgRepository;

    @Mock
    private ValidationResources validationResources;

    @Mock
    private FireBaseService fireBaseService;

    @InjectMocks
    private FileUploadService fileUploadService;

    // DDT CSV file: src/test/resources/fileupload.csv
    // Columns: creatorId,imgName,imgContent,type,expectedResult
    @ParameterizedTest
    @CsvFileSource(resources = "/data/fileupload.csv", numLinesToSkip = 1)
    void testSaveImgIntoFirebase(long creatorId, String imgName, String imgContent, String type, String expectedResult) throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", imgName, "image/png", imgContent.getBytes());

        Creator creator = Creator.builder().creatorId(creatorId).build();

        when(validationResources.validateCreatorExists(creatorId)).thenReturn(creator);

        if (expectedResult.equals("EXCEPTION")) {
            when(fireBaseService.uploadFile(any(), any())).thenThrow(new IOException("Upload failed"));
            assertThrows(ApiException.class, () -> fileUploadService.saveImgIntoFirebase(creatorId, file, TypeOfFile.valueOf(type)));
        } else {
            when(fireBaseService.uploadFile(any(), any())).thenReturn(expectedResult);

            String url = fileUploadService.saveImgIntoFirebase(creatorId, file, TypeOfFile.valueOf(type));

            assertEquals(expectedResult, url);
            verify(pendingImgRepository, times(1)).save(any(PendingImage.class));
        }
    }
}
