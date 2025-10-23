package com.jpd.web.model;

public enum ReportType {
	INAPPROPRIATE_CONTENT,//Nội dung phản cảm, tục tĩu, không phù hợp
	MISLEADING_INFORMATION,//Thông tin sai lệch hoặc gây hiểu nhầm
	COPYRIGHT_VIOLATION,//Vi phạm bản quyền
	DISCRIMINATION_OR_HATE,//Ngôn từ thù ghét hoặc phân biệt đối xử
	POOR_QUALITY,//Chất lượng khóa học kém
	SCAM_OR_FRAUD,//Lừa đảo hoặc yêu cầu thanh toán bất hợp pháp
	RELIGIOUS_OR_POLITICAL_CONTENT,//Nội dung tôn giáo hoặc chính trị không phù hợp
	OTHER//Khác
}
