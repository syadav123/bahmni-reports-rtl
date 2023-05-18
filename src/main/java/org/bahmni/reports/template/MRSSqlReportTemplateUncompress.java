package org.bahmni.reports.template;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bahmni.reports.model.Report;
import org.bahmni.reports.model.SqlReportConfig;
import org.bahmni.reports.model.UsingDatasource;
import org.bahmni.reports.report.BahmniReportBuilder;
import org.bahmni.reports.util.CommonComponents;
import org.bahmni.reports.util.LZString;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.datatype.DataTypes;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.WhenNoDataType;
import net.sf.dynamicreports.report.definition.datatype.DRIDataType;

@UsingDatasource("openmrs")
public class MRSSqlReportTemplateUncompress extends SqlReportTemplate {

    @Override
    public BahmniReportBuilder build(Connection connection, JasperReportBuilder jasperReport, Report<SqlReportConfig> report, String
            startDate, String endDate, List<AutoCloseable> resources, PageType pageType) {
        CommonComponents.addTo(jasperReport, report, pageType);

        String sqlString = getSqlString(report, startDate, endDate);
        ResultSet resultSet = null;
        Statement statement = null;
        ResultSetMetaData metaData;
        int columnCount;
        List<Map<String, Object>> resultsModded = new ArrayList<>();

        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sqlString);
            metaData = resultSet.getMetaData();
            columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                jasperReport.addColumn(col.column(metaData.getColumnLabel(i), metaData.getColumnName(i), mapSqlDataTypeToJasperDataType(metaData.getColumnType(i))));
            }

            while(resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    int colType = metaData.getColumnType(i);
                    DRIDataType jasperDataType = mapSqlDataTypeToJasperDataType(colType);
                    if (jasperDataType == DataTypes.stringType()) {
                        String origCompress = resultSet.getString(i);
                        String origUncompress = null;
                        try {
                            origUncompress = LZString.decompressFromEncodedURIComponent(origCompress);
                        } catch (Exception ex) {
                            origUncompress = origCompress;
                        }
                        if (StringUtils.isBlank(origUncompress) || "null".equalsIgnoreCase(origUncompress)) {
                            origUncompress = origCompress;
                        }
                        if (StringUtils.isBlank(origUncompress)) {
                            origUncompress = "";
                        }
                        String content = new String(origUncompress.getBytes(), "UTF-8");
                        row.put(metaData.getColumnLabel(i), content);
                    } else {
                        row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                    }
                }
                resultsModded.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        jasperReport.setDataSource(resultsModded);
        jasperReport.setWhenNoDataType(WhenNoDataType.ALL_SECTIONS_NO_DETAIL);
        resources.add(statement);
        return new BahmniReportBuilder(jasperReport);
    }

    private DRIDataType mapSqlDataTypeToJasperDataType(int sqlType) {
        switch(sqlType){
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return DataTypes.integerType();

            case Types.BIGINT:
                return DataTypes.longType();

            case Types.FLOAT:
                return DataTypes.floatType();
            case Types.DOUBLE:
                return DataTypes.doubleType();
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DataTypes.dateType();
            default:
                return DataTypes.stringType();
        }
    }

}
