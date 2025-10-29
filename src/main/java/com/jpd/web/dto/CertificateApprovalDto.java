package com.jpd.web.dto;

import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateApprovalDto {
    private Long creatorId;
    private String fullName;
    private List<String> certificateUrls;
    private Date submittedAt;
    private Status status;
    private String adminNote;
}
