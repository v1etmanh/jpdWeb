package com.jpd.web.UnitTest;

import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Module;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.ModuleService;
import com.jpd.web.service.utils.ValidationResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService - DDT with actual updateModuleName() logic (void)")
public class ModuleServiceUnitTest {

    @Mock
    ValidationResources validationResources;

    @Mock
    ModuleRepository moduleRepository;

    @Mock
    ModuleContentRepository moduleContentRepository;

    @InjectMocks
    ModuleService moduleService;

    // helper đọc CSV
    private List<Map<String, String>> readCsv(String path) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream()))) {
            String header = br.readLine();
            if (header == null) return Collections.emptyList();
            String[] keys = header.split(",");
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] vals = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < keys.length; i++) {
                    row.put(keys[i].trim(), i < vals.length ? vals[i].trim() : "");
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private Long safeLong(String s, long def) {
        try {
            return (s == null || s.isBlank()) ? def : Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    // -------------------------
    // updateModuleName() - DDT (matches actual service)
    // -------------------------
    @Nested
    @DisplayName("updateModuleName() - based on actual ModuleService implementation")
    class UpdateModuleDDT {
        @TestFactory
        Collection<DynamicTest> tests() throws Exception {
            List<Map<String, String>> rows = readCsv("data/module-update.csv");
            return rows.stream().map(r ->
                    DynamicTest.dynamicTest(r.getOrDefault("testCase", "update"), () -> {

                        long callerId = safeLong(r.get("creatorId"), 1);
                        long moduleId = safeLong(r.get("moduleId"), 1);
                        String newTitle = r.getOrDefault("newTitle", "Updated");
                        boolean shouldSucceed = Boolean.parseBoolean(r.getOrDefault("shouldSucceed", "true"));
                        String expectedError = r.getOrDefault("expectedError", "");

                        // reset mocks for safety
                        reset(validationResources, moduleRepository);

                        if (shouldSucceed) {
                            // ✅ Success case: caller và owner phải trùng creatorId
                            Creator caller = new Creator();
                            ReflectionTestUtils.setField(caller, "creatorId", callerId);

                            Creator owner = new Creator();
                            ReflectionTestUtils.setField(owner, "creatorId", callerId); // cùng ID với caller

                            Course course = new Course();
                            course.setCreator(owner);

                            Chapter chapter = new Chapter();
                            chapter.setCourse(course);

                            Module module = new Module();
                            module.setModuleId(moduleId);
                            module.setChapter(chapter);
                            module.setTitleOfModule("Old Title");

                            when(validationResources.validateCreatorExists(callerId)).thenReturn(caller);
                            when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
                            when(moduleRepository.save(any(Module.class))).thenAnswer(inv -> inv.getArgument(0));

                            assertDoesNotThrow(() -> moduleService.updateModuleName(callerId, moduleId, newTitle));

                            // verify save được gọi và title được cập nhật
                            verify(moduleRepository).save(argThat(m -> newTitle.equals(m.getTitleOfModule())));
                        }
                        else if ("ModuleNotFoundException".equals(expectedError)) {
                            // ❌ Module không tồn tại
                            Creator caller = new Creator();
                            ReflectionTestUtils.setField(caller, "creatorId", callerId);

                            when(validationResources.validateCreatorExists(callerId)).thenReturn(caller);
                            when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

                            assertThrows(ModuleNotFoundException.class,
                                    () -> moduleService.updateModuleName(callerId, moduleId, newTitle));
                        }
                        else if ("UnauthorizedException".equals(expectedError)) {
                            // ❌ Module thuộc về creator khác
                            Creator caller = new Creator();
                            ReflectionTestUtils.setField(caller, "creatorId", callerId);

                            Creator owner = new Creator();
                            ReflectionTestUtils.setField(owner, "creatorId", callerId + 999);

                            Course course = new Course();
                            course.setCreator(owner);

                            Chapter chapter = new Chapter();
                            chapter.setCourse(course);

                            Module module = new Module();
                            module.setModuleId(moduleId);
                            module.setChapter(chapter);

                            when(validationResources.validateCreatorExists(callerId)).thenReturn(caller);
                            when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

                            assertThrows(UnauthorizedException.class,
                                    () -> moduleService.updateModuleName(callerId, moduleId, newTitle));
                        }
                        else {
                            // fallback
                            Creator caller = new Creator();
                            ReflectionTestUtils.setField(caller, "creatorId", callerId);

                            when(validationResources.validateCreatorExists(callerId)).thenReturn(caller);
                            when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());
                            assertThrows(Exception.class,
                                    () -> moduleService.updateModuleName(callerId, moduleId, newTitle));
                        }

                        // cleanup stubs for next row
                        reset(validationResources, moduleRepository);
                    })
            ).collect(Collectors.toList());
        }
    }
}
