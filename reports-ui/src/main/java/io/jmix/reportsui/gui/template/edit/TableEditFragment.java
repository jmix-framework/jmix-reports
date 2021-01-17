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

package io.jmix.reportsui.gui.template.edit;

import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.Sort;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.entity.table.TemplateTableBand;
import io.jmix.reports.entity.table.TemplateTableColumn;
import io.jmix.reports.entity.table.TemplateTableDescription;
import io.jmix.ui.Actions;
import io.jmix.ui.Notifications;
import io.jmix.ui.RemoveOperation;
import io.jmix.ui.action.Action;
import io.jmix.ui.action.list.CreateAction;
import io.jmix.ui.action.list.RemoveAction;
import io.jmix.ui.component.BoxLayout;
import io.jmix.ui.component.Table;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.Install;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;

@UiController("report_TableEdit.fragment")
@UiDescriptor("table-edit-frame.xml")
public class TableEditFragment extends DescriptionEditFragment {

    public static final int UP = 1;
    public static final int DOWN = -1;
    public static final String POSITION = "position";

    protected ReportTemplate reportTemplate;
    protected TemplateTableDescription description;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected Table<TemplateTableBand> bandsTable;

    @Autowired
    protected Table<TemplateTableColumn> columnsTable;

    @Autowired
    protected CollectionContainer<TemplateTableBand> tableBandsDc;

    @Autowired
    protected InstanceContainer<TemplateTableDescription> templateTableDc;

    @Autowired
    protected CollectionContainer<TemplateTableColumn> tableColumnsDc;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Notifications notifications;

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    @Subscribe
    protected void onInit(InitEvent event) {
        super.onInit(event);
    }

    protected void sortParametersByPosition(CollectionContainer collectionContainer) {
        collectionContainer.getSorter().sort(Sort.by(Sort.Direction.ASC, POSITION));
    }

    @Install(to = "bandsTable.remove", subject = "afterActionPerformedHandler")
    protected void bandsTableRemoveAfterActionPerformedHandler(RemoveOperation.AfterActionPerformedEvent<TemplateTableBand> event) {
        TemplateTableBand deletedColumn = event.getItems().iterator().next();
        int deletedPosition = deletedColumn.getPosition();

        for (TemplateTableBand templateTableBand : tableBandsDc.getItems()) {
            if (templateTableBand.getPosition() > deletedPosition) {
                int currentPosition = templateTableBand.getPosition();
                templateTableBand.setPosition(currentPosition - 1);
            }
        }
    }

    @Subscribe("bandsTable.create")
    protected void onBandsTableCreate(Action.ActionPerformedEvent event) {
        TemplateTableBand templateTableBand = metadata.create(TemplateTableBand.class);
        templateTableBand.setPosition(tableBandsDc.getItems().size());

        tableBandsDc.getMutableItems().add(templateTableBand);
    }

    @Subscribe("bandsTable.downBand")
    protected void onBandsTableDownBand(Action.ActionPerformedEvent event) {
        changeOrderBandsOfIndexes(DOWN);
    }

    @Install(to = "bandsTable.downBand", subject = "enabledRule")
    protected boolean bandsTableDownBandEnabledRule() {
        TemplateTableBand item = bandsTable.getSingleSelected();
        if (item == null) {
            return false;
        }
        return item.getPosition() < (tableBandsDc.getItems().size() - 1);
    }

    @Subscribe("bandsTable.upBand")
    protected void onBandsTableUpBand(Action.ActionPerformedEvent event) {
        changeOrderBandsOfIndexes(UP);
    }

    @Install(to = "bandsTable.upBand", subject = "enabledRule")
    protected boolean bandsTableUpBandEnabledRule() {
        TemplateTableBand item = bandsTable.getSingleSelected();
        if (item == null) {
            return false;
        }
        return item.getPosition() > 0;
    }

    @Subscribe("columnsTable.create")
    protected void onColumnsTableCreate(Action.ActionPerformedEvent event) {
        TemplateTableBand selectBand = bandsTable.getSingleSelected();

        if (selectBand != null) {
            TemplateTableColumn item = metadata.create(TemplateTableColumn.class);
            item.setPosition(tableColumnsDc.getItems().size());

            tableColumnsDc.getMutableItems().add(item);
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage("template.bandRequired"))
                    .show();
        }
    }

    @Install(to = "columnsTable.remove", subject = "afterActionPerformedHandler")
    protected void columnsTableRemoveAfterActionPerformedHandler(RemoveOperation.AfterActionPerformedEvent<TemplateTableColumn> event) {
        TemplateTableColumn deletedColumn = event.getItems().iterator().next();
        int deletedPosition = deletedColumn.getPosition();

        for (TemplateTableColumn templateTableColumn : tableColumnsDc.getItems()) {
            if (templateTableColumn.getPosition() > deletedPosition) {
                int currentPosition = templateTableColumn.getPosition();
                templateTableColumn.setPosition(currentPosition - 1);
            }
        }
    }

    @Subscribe("columnsTable.upColumn")
    protected void onColumnsTableUpColumn(Action.ActionPerformedEvent event) {
        changeOrderColumnsOfIndexes(UP);
    }

    @Install(to = "columnsTable.upColumn", subject = "enabledRule")
    protected boolean columnsTableUpColumnEnabledRule() {
        TemplateTableColumn item = columnsTable.getSingleSelected();
        if (item == null) {
            return false;
        }
        return item.getPosition() > 0;
    }

    @Subscribe("columnsTable.downColumn")
    protected void onColumnsTableDownColumn(Action.ActionPerformedEvent event) {
        changeOrderColumnsOfIndexes(DOWN);
    }

    @Install(to = "columnsTable.downColumn", subject = "enabledRule")
    protected boolean columnsTableDownColumnEnabledRule() {
        TemplateTableColumn item = columnsTable.getSingleSelected();
        if (item == null) {
            return false;
        }
        return item.getPosition() < (tableColumnsDc.getItems().size() - 1);
    }

    private void changeOrderColumnsOfIndexes(int order) {
        TemplateTableColumn currentColumn = columnsTable.getSingleSelected();
        int currentPosition = currentColumn.getPosition();

        for (TemplateTableColumn templateTableColumn : tableColumnsDc.getItems()) {
            if (templateTableColumn.getPosition() == (currentPosition - order)) {
                templateTableColumn.setPosition(templateTableColumn.getPosition() + order);
            }
            currentColumn.setPosition(currentPosition - order);
        }
        sortParametersByPosition(tableColumnsDc);
    }

    private void changeOrderBandsOfIndexes(int order) {
        TemplateTableBand currentBand = bandsTable.getSingleSelected();
        int currentPosition = currentBand.getPosition();

        for (TemplateTableBand templateTableBand : tableBandsDc.getItems()) {
            if (templateTableBand.getPosition() == (currentPosition - order)) {
                templateTableBand.setPosition(templateTableBand.getPosition() + order);
            }
            currentBand.setPosition(currentPosition - order);
        }
        sortParametersByPosition(tableBandsDc);
    }


    protected TemplateTableDescription createDefaultTemplateTableDescription() {
        description = metadata.create(TemplateTableDescription.class);
        return description;
    }


    public void setItem(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;

        if (reportTemplate.getTemplateTableDescription() == null) {
            templateTableDc.setItem(createDefaultTemplateTableDescription());
        } else {
            templateTableDc.setItem(reportTemplate.getTemplateTableDescription());
        }

    }

    @Override
    public boolean applyChanges() {
        reportTemplate.setTemplateTableDescription(templateTableDc.getItem());

        for (TemplateTableBand band : tableBandsDc.getItems()) {
            if (band.getBandName() == null) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage("template.bandTableOrColumnTableRequired"))
                        .show();
                return false;
            }

            for (TemplateTableColumn column : band.getColumns()) {
                if (column.getKey() == null || column.getCaption() == null) {
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage("template.bandTableOrColumnTableRequired"))
                            .show();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isApplicable(ReportOutputType reportOutputType) {
        return reportOutputType == ReportOutputType.TABLE;
    }

    @Override
    protected void initPreviewContent(BoxLayout previewBox) {
    }

    @Override
    public boolean isSupportPreview() {
        return false;
    }
}
