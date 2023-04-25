package org.bahmni.reports.web;


import java.util.ResourceBundle;
import java.io.UnsupportedEncodingException;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.component.HorizontalListBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;

import org.apache.commons.lang3.StringUtils;
import org.bahmni.reports.template.BaseReportTemplate;
import org.bahmni.reports.template.Templates;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;

public class ReportHeader {

    private String reportTimeZone;
    private ResourceBundle reportLocaleBundle = null;
   
    public ReportHeader() {
        this.reportTimeZone = ZoneId.systemDefault().getId();
    }

    public ReportHeader(String reportTimeZone) {
        this.reportTimeZone = reportTimeZone;
    }

    public JasperReportBuilder add(JasperReportBuilder jasperReportBuilder, String reportName, String startDate, String endDate) throws DRException, UnsupportedEncodingException {
        HorizontalListBuilder headerList = cmp.horizontalList();
        reportLocaleBundle = BaseReportTemplate.getLocaleBundle();

        addTitle(reportName, headerList);

        addDatesSubHeader(startDate, endDate, headerList);

        addReportGeneratedDateSubHeader(headerList);

        addVerticalGap(headerList);

        jasperReportBuilder.addTitle(headerList);

        return jasperReportBuilder;
    }

    private void addVerticalGap(HorizontalListBuilder headerList) {
        headerList.add(cmp.line())
                .add(cmp.verticalGap(10));
    }

    private void addTitle(String reportName, HorizontalListBuilder headerList) {
        headerList.add(cmp.text(reportName)
                .setStyle(Templates.bold18CenteredStyle)
                .setHorizontalAlignment(HorizontalAlignment.CENTER))
                .newRow()
                .add(cmp.verticalGap(5));
    }

    private void addDatesSubHeader(String startDate, String endDate, HorizontalListBuilder headerList) throws DRException, UnsupportedEncodingException {
        if (startDate.equalsIgnoreCase("null") || endDate.equalsIgnoreCase("null")) return;
        String msgFrom = new String(reportLocaleBundle.getString("From_report").getBytes("8859_1"), "UTF-8");
        String msgTo = new String(reportLocaleBundle.getString("To_report").getBytes("8859_1"), "UTF-8");

        headerList.add(cmp.text(msgFrom + " " + startDate + " " + msgTo + " " + endDate)
                .setStyle(Templates.bold12CenteredStyle)
                .setHorizontalAlignment(HorizontalAlignment.CENTER))
                .newRow();
    }

    private void addReportGeneratedDateSubHeader(HorizontalListBuilder headerList) throws UnsupportedEncodingException {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("yyyy-MM-dd hh:mm:ss")
                .toFormatter();

        String msg = "Report_Generated_On";
        String newMsg = new String(reportLocaleBundle.getString(msg).getBytes("8859_1"), "UTF-8");


        if (StringUtils.isBlank(reportTimeZone)) {
            reportTimeZone = ZoneId.systemDefault().getId();
        }
        ZoneId rZone = ZoneId.of(reportTimeZone);
        ZonedDateTime nowLocalTime = ZonedDateTime.now(rZone);
        String dateString = formatter.format(nowLocalTime);
        headerList.add(cmp.text(newMsg + ": " + dateString)
                .setStyle(Templates.bold12CenteredStyle)
                .setHorizontalAlignment(HorizontalAlignment.CENTER))
                .newRow();
    }

}
