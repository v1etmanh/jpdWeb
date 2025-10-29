package com.jpd.web.transform;

import com.jpd.web.dto.ReportForm;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Report;

public class ReportTransform {
public static Report transToReport(ReportForm reportForm,Course c,Customer cus)
{return Report.builder()
		.customer(cus)
		.course(c)
		.detail(reportForm.getDetail())
		.type(reportForm.getType())
		.build();
	}
}
