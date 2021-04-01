/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.reportsui.screen.report.wizard.template.generators;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.jmix.reports.entity.wizard.RegionProperty;
import io.jmix.reports.entity.wizard.ReportData;
import io.jmix.reports.entity.wizard.ReportRegion;
import io.jmix.reportsui.screen.report.wizard.ReportingWizardImpl;
import io.jmix.reportsui.screen.report.wizard.template.Generator;
import io.jmix.reportsui.screen.report.wizard.template.ReportTemplatePlaceholder;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HtmlGenerator implements Generator {
    protected ReportTemplatePlaceholder reportTemplatePlaceholder = new ReportTemplatePlaceholder();
    protected static final String HTML_TEMPLATE_NAME = "defaultTemplate";
    protected static final String HTML_TEMPLATE_PLACEHOLDER_TITLE = "title";
    protected static final String HTML_TEMPLATE_PLACEHOLDER_STYLES = "styles";
    protected static final String HTML_TEMPLATE_PLACEHOLDER_BODY = "body";
    protected volatile static Configuration freeMarkerConfiguration;
    protected ReportData reportData;

    @Override
    public byte[] generate(ReportData reportData) throws TemplateException, IOException {
        this.reportData = reportData;
        byte[] template;
        Configuration conf = getFreemarkerConfiguration();
        Template freeMarkerHtmlReportTemplate = conf.getTemplate(HTML_TEMPLATE_NAME);
        StringWriter out = new StringWriter(2048);
        Map<String, String> templateParameters = new HashMap<>();
        putTitleHtml(templateParameters);
        putStylesHtml(templateParameters);
        putBodyHtml(templateParameters);
        freeMarkerHtmlReportTemplate.process(templateParameters, out);
        template = out.toString().getBytes(StandardCharsets.UTF_8);
        return template;
    }

    protected void putTitleHtml(Map<String, String> templateParameters) {
        templateParameters.put(HTML_TEMPLATE_PLACEHOLDER_TITLE, reportData.getName());

    }

    protected void putStylesHtml(Map<String, String> templateParameters) {
        templateParameters.put(HTML_TEMPLATE_PLACEHOLDER_STYLES, " body  {font-family: 'Charis SIL', sans-serif;}\n tbody tr {height:20px; min-height:20px}\n");
    }

    protected void putBodyHtml(Map<String, String> templateParameters) {
        StringBuilder templateBody = new StringBuilder();
        //Add #assign statements:
        for (ReportRegion reportRegion : reportData.getReportRegions()) {
            //header of table is filled here, so the three lines of code below is unused:
            appendHtmlFreeMarkerAssignments(templateBody, reportRegion.getNameForBand());
        }

        appendFreeMarkerSettings(templateBody);

        for (ReportRegion reportRegion : reportData.getReportRegions()) {
            if (reportRegion.isTabulatedRegion()) {
                //Are U ready for a String porn?
                //table def
                templateBody.append("\n\n<table class=\"report-table\" border=\"1\" cellspacing=\"0\" >\n");
                //table header
                templateBody.append("<thead>\n<tr>\n");
                for (RegionProperty regionProperty : reportRegion.getRegionProperties()) {
                    templateBody.append("<th>").append(regionProperty.getHierarchicalLocalizedNameExceptRoot()).append("</th>\n");
                }
                //closing table header tags:
                templateBody.append("</tr>\n</thead>\n");
                //table body rows
                templateBody.append("<tbody>\n<#if ").append(reportRegion.getNameForBand()).append("?has_content>\n<#list ").
                        append(reportRegion.getNameForBand()).
                        append(" as row>\n<tr>");
                for (RegionProperty regionProperty : reportRegion.getRegionProperties()) {
                    templateBody.append("\n<td> ").append(reportTemplatePlaceholder.getHtmlPlaceholderValue(reportRegion, regionProperty)).append(" </td>");
                }
                //closing table and table body tags:
                templateBody.append("\n</tr>\n</#list>\n</#if>\n</tbody>\n</table>\n\n");
            } else {
                for (RegionProperty regionProperty : reportRegion.getRegionProperties()) {
                    templateBody.append("\n").
                            append("<p>").
                            append(regionProperty.getHierarchicalLocalizedNameExceptRoot()).
                            append(": ").
                            append(reportTemplatePlaceholder.getHtmlPlaceholderValue(reportRegion, regionProperty)).append("</p>");
                }
            }
        }
        templateParameters.put(HTML_TEMPLATE_PLACEHOLDER_BODY, templateBody.toString());
    }

    protected void appendFreeMarkerSettings(StringBuilder templateBody) {
        //todo
//        Messages messages = AppBeans.get(Messages.NAME);
//        templateBody.append("\n<#setting boolean_format=\"").
//                append(messages.getMainMessage( "trueString")).
//                append(",").
//                append(messages.getMainMessage( "falseString")).
//                append("\">");
    }

    protected void appendHtmlFreeMarkerAssignments(StringBuilder stringBuilder, String bandName) {
        stringBuilder.append("\n<#assign ").
                append(bandName).
                append(" = ").
                append(ReportingWizardImpl.ROOT_BAND_DEFINITION_NAME).
                append(".bands.").
                append(bandName).
                append("><br/>");
    }

    protected Configuration getFreemarkerConfiguration() {
        if (freeMarkerConfiguration == null) {
            synchronized (this) {
                if (freeMarkerConfiguration == null) {
                    freeMarkerConfiguration = new Configuration();
                    StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
                    stringTemplateLoader.putTemplate(HTML_TEMPLATE_NAME, getReportTemplateHtmlFreeMarkerTemplate());
                    freeMarkerConfiguration.setTemplateLoader(stringTemplateLoader);
                }
            }
        }
        return freeMarkerConfiguration;
    }

    protected String getReportTemplateHtmlFreeMarkerTemplate() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ru\">\n" +
                "    <head>\n" +
                "        <title> ${" + HTML_TEMPLATE_PLACEHOLDER_TITLE + "!\"Html template\"} </title>\n" +
                "        <style type=\"text/css\">\n" +
                "            ${" + HTML_TEMPLATE_PLACEHOLDER_STYLES + "!\"<!--put Your styles here-->\"}\n" +
                "        </style>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        ${" + HTML_TEMPLATE_PLACEHOLDER_BODY + "!\"\"}\n" +
                "    </body>\n" +
                "</html>";
    }

}