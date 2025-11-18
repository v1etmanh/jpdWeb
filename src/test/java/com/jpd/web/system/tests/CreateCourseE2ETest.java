package com.jpd.web.system.tests;

import com.jpd.web.system.pages.CreateCoursePage;
import com.jpd.web.system.pages.CreatorSidebar;
import com.jpd.web.system.utils.ResourceUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCourseE2ETest extends BaseTest {

    @Test
    @DisplayName("Open Creator Sidebar -> Create Your Course -> fill steps -> upload image -> submit")
    public void createCoursePublic_success() {
        CreatorSidebar sidebar = new CreatorSidebar(driver);
        sidebar.openCreateCourseViaSidebar();

        CreateCoursePage page = new CreateCoursePage(driver);

        // Step 1
        page.fillStep1(
                "Khóa học Selenium 4 từ cơ bản đến nâng cao",
                "Khóa học chuyên sâu về Selenium WebDriver 4 + JUnit 5 + POM, thực hành đầy đủ.",
                "Tiếng Việt",
                "Tiếng Việt"
        );
        page.next();

        // Step 2
        page.fillStep2(
                "Người mới bắt đầu automation test, QA chuyển hướng",
                "Kiến thức cơ bản về Java, hiểu về HTML/CSS",
                "Viết được test E2E theo mô hình POM, áp dụng wait thông minh"
        );
        page.next();

        // Step 3
        String imgPath = ResourceUtil.pathInResources("test-course.png");
        page.fillStep3("Công khai (Miễn phí)", imgPath);
        page.submit();

        boolean ok = page.waitForSuccessOrRedirect();
        assertThat(ok).as("Expect success toast or redirect to /creator/courseList").isTrue();
    }
}
