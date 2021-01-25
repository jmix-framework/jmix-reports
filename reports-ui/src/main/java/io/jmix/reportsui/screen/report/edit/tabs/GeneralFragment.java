package io.jmix.reportsui.screen.report.edit.tabs;

import io.jmix.core.*;
import io.jmix.reports.entity.*;
import io.jmix.reportsui.screen.definition.edit.BandDefinitionEditor;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenBuilders;
import io.jmix.ui.Screens;
import io.jmix.ui.UiProperties;
import io.jmix.ui.action.Action;
import io.jmix.ui.app.file.FileUploadDialog;
import io.jmix.ui.component.*;
import io.jmix.ui.download.ByteArrayDataProvider;
import io.jmix.ui.download.DownloadFormat;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.model.InstanceLoader;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.util.*;

@UiController("report_ReportEditGeneral.fragment")
@UiDescriptor("general.xml")
public class GeneralFragment extends ScreenFragment {

    @Named("serviceTree")
    protected Tree<BandDefinition> bandTree;

    @Autowired
    protected InstanceLoader<Report> reportDl;

    @Autowired
    protected InstanceContainer<Report> reportDc;

    @Autowired
    protected CollectionContainer<BandDefinition> bandsDc;

    @Autowired
    protected CollectionContainer<BandDefinition> availableParentBandsDc;

    @Autowired
    protected CollectionPropertyContainer<ReportTemplate> templatesDc;

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected CollectionPropertyContainer<DataSet> dataSetsDc;

    @Autowired
    protected EntityStates entityStates;

    @Autowired
    protected Notifications notifications;

    @Autowired
    protected Messages messages;

    @Autowired
    protected Downloader downloader;

    @Autowired
    protected UiProperties uiProperties;

    @Autowired
    protected CoreProperties coreProperties;

    @Autowired
    protected Screens screens;

    @Autowired
    protected ScreenBuilders screenBuilders;

    @Autowired
    protected BandDefinitionEditor bandEditor;

    @Autowired
    protected FileUploadField invisibleFileUpload;

    @Autowired
    private HBoxLayout reportFields;

    @Autowired
    protected Button up;

    @Autowired
    protected Button down;

    @Autowired
    protected EntityComboBox<ReportTemplate> defaultTemplateField;

    @Subscribe("invisibleFileUpload")
    protected void onInvisibleFileUploadFileUploadSucceed(SingleFileUploadField.FileUploadSucceedEvent event) {
        final ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
        if (defaultTemplate != null) {
            if (!isTemplateWithoutFile(defaultTemplate)) {
                //todo
//                    File file = fileUpload.getFile(invisibleFileUpload.getFileName());
//                    try {
//                        byte[] data = FileUtils.readFileToByteArray(file);
//                        defaultTemplate.setContent(data);
//                        defaultTemplate.setName(invisibleFileUpload.getFileName());
//                        templatesDc.modifyItem(defaultTemplate);
//                    } catch (IOException e) {
//                        throw new RuntimeException(String.format(
//                                "An error occurred while uploading file for template [%s]",
//                                defaultTemplate.getCode()));
//                    }
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(getClass(), "notification.fileIsNotAllowedForSpecificTypes"))
                        .show();
            }
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage(getClass(), "notification.defaultTemplateIsEmpty"))
                    .show();
        }
    }

    @Subscribe(id = "bandsDc", target = Target.DATA_CONTAINER)
    protected void onBandsDcItemChange(InstanceContainer.ItemChangeEvent<BandDefinition> event) {
        bandEditor.setEnabled(event.getItem() != null);
        availableParentBandsDc.getMutableItems().clear();
        if (event.getItem() != null) {
            for (BandDefinition bandDefinition : bandsDc.getItems()) {
                if (!isChildOrEqual(event.getItem(), bandDefinition) ||
                        Objects.equals(event.getItem().getParentBandDefinition(), bandDefinition)) {
                    availableParentBandsDc.getMutableItems().add(bandDefinition);
                }
            }
        }
    }

    @Subscribe(id = "bandsDc", target = Target.DATA_CONTAINER)
    protected void onBandsDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<BandDefinition> event) {
        if ("parentBandDefinition".equals(event.getProperty())) {
            BandDefinition previousParent = (BandDefinition) event.getPrevValue();
            BandDefinition parent = (BandDefinition) event.getValue();

            if (event.getValue() == event.getItem()) {
                event.getItem().setParentBandDefinition(previousParent);
            } else {
                previousParent.getChildrenBandDefinitions().remove(event.getItem());
                parent.getChildrenBandDefinitions().add(event.getItem());
            }

            if (event.getPrevValue() != null) {
                orderBandDefinitions(previousParent);
            }

            if (event.getValue() != null) {
                orderBandDefinitions(parent);
            }
        }
    }

    @Subscribe
    protected void onInit(InitEvent event) {
        defaultTemplateField.setEditable(isUpdatePermitted());
    }

    @Subscribe("defaultTemplateField.create")
    protected void onDefaultTemplateFieldCreate(Action.ActionPerformedEvent event) {
        Report report = reportDc.getItem();
        ReportTemplate template = metadata.create(ReportTemplate.class);
        template.setReport(report);

        StandardEditor editor = (StandardEditor) screenBuilders.editor(defaultTemplateField)
                .withScreenId("report_ReportTemplate.edit")
                .withContainer(templatesDc)
                .withOpenMode(OpenMode.DIALOG)
                .editEntity(template)
                .build();

        editor.addAfterCloseListener(e -> {
            StandardCloseAction standardCloseAction = (StandardCloseAction) e.getCloseAction();
            if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
                ReportTemplate item = (ReportTemplate) editor.getEditedEntity();
                templatesDc.getMutableItems().add(item);
                report.setDefaultTemplate(item);
                //Workaround to disable button after default template
                //TODO
//                Action defaultTemplate = templatesTable.getActionNN("defaultTemplate");
//                defaultTemplate.refreshState();
            }
            defaultTemplateField.focus();
        });
        editor.show();
    }

    @Install(to = "defaultTemplateField.create", subject = "enabledRule")
    protected boolean defaultTemplateFieldCreateEnabledRule() {
        return isUpdatePermitted();
    }

    @Install(to = "defaultTemplateField.edit", subject = "enabledRule")
    protected boolean defaultTemplateFieldEditEnabledRule() {
        return isUpdatePermitted();
    }

    @Subscribe("defaultTemplateField.edit")
    protected void onDefaultTemplateFieldEdit(Action.ActionPerformedEvent event) {
        Report report = reportDc.getItem();
        ReportTemplate defaultTemplate = report.getDefaultTemplate();
        if (defaultTemplate != null) {
            StandardEditor editor = (StandardEditor) screenBuilders.editor(defaultTemplateField)
                    .withScreenId("report_ReportTemplate.edit")
                    .withOpenMode(OpenMode.DIALOG)
                    .withContainer(templatesDc)
                    .editEntity(defaultTemplate)
                    .build();

            editor.addAfterCloseListener(e -> {
                StandardCloseAction standardCloseAction = (StandardCloseAction) e.getCloseAction();
                if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
                    ReportTemplate item = (ReportTemplate) editor.getEditedEntity();
                    report.setDefaultTemplate(item);
                }
                defaultTemplateField.focus();
            });
            editor.show();
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage(getClass(), "notification.defaultTemplateIsEmpty"))
                    .show();
        }
    }

    @Subscribe("defaultTemplateField")
    protected void onDefaultTemplateFieldValueChange(HasValue.ValueChangeEvent<ReportTemplate> event) {
        setupDropZoneForTemplate();
    }

    @Subscribe("defaultTemplateField.upload")
    protected void onDefaultTemplateUpload(Action.ActionPerformedEvent event) {
        final ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
        if (defaultTemplate != null) {
            if (!isTemplateWithoutFile(defaultTemplate)) {
                FileUploadDialog fileUploadDialog = (FileUploadDialog) screenBuilders.screen(getFragment().getFrameOwner())
                        .withScreenId("singleFileUploadDialog")
                        .withOpenMode(OpenMode.DIALOG)
                        .build();

                fileUploadDialog.addAfterCloseListener(closeEvent -> {
                    StandardCloseAction standardCloseAction = (StandardCloseAction) closeEvent.getCloseAction();
                    if (Window.COMMIT_ACTION_ID.equals(standardCloseAction.getActionId())) {
                        //todo
//                                        File file = fileUpload.getFile(fileUploadDialog.getFileId());
//                                        try {
//                                            byte[] data = FileUtils.readFileToByteArray(file);
//                                            defaultTemplate.setContent(data);
//                                            defaultTemplate.setName(fileUploadDialog.getFileName());
//                                            //todo
//                                            //templatesDc.modifyItem(defaultTemplate);
//                                        } catch (IOException e) {
//                                            throw new RuntimeException(String.format(
//                                                    "An error occurred while uploading file for template [%s]",
//                                                    defaultTemplate.getCode()));
//                                        }
                    }
                    defaultTemplateField.focus();
                });
                fileUploadDialog.show();

            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(getClass(), "notification.fileIsNotAllowedForSpecificTypes"))
                        .show();
            }
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage(getClass(), "notification.defaultTemplateIsEmpty"))
                    .show();
        }
    }

    @Install(to = "defaultTemplateField.upload", subject = "enabledRule")
    protected boolean defaultTemplateFieldUploadEnabledRule() {
        return isUpdatePermitted();
    }

    @Subscribe("defaultTemplateField.download")
    protected void onDefaultTemplateDownload(Action.ActionPerformedEvent event) {
        ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
        if (defaultTemplate != null) {
            if (defaultTemplate.isCustom()) {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(getClass(), "unableToSaveTemplateWhichDefinedWithClass"))
                        .show();
            } else if (isTemplateWithoutFile(defaultTemplate)) {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption(messages.getMessage(getClass(), "notification.fileIsNotAllowedForSpecificTypes"))
                        .show();
            } else {
                byte[] reportTemplate = defaultTemplate.getContent();
                downloader.download(new ByteArrayDataProvider(reportTemplate, uiProperties.getSaveExportedByteArrayDataThresholdBytes(), coreProperties.getTempDir()),
                        defaultTemplate.getName(), DownloadFormat.getByExtension(defaultTemplate.getExt()));
            }
        } else {
            notifications.create(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messages.getMessage(getClass(), "notification.defaultTemplateIsEmpty"))
                    .show();
        }

        defaultTemplateField.focus();
    }


    @Install(to = "serviceTree.upAction", subject = "enabledRule")
    protected boolean serviceTreeUpActionEnabledRule() {
        return isUpButtonEnabled();
    }

    @Install(to = "serviceTree.downAction", subject = "enabledRule")
    protected boolean serviceTreeDownActionEnabledRule() {
        return isDownButtonEnabled();
    }

    protected void sortBandDefinitionsTableByPosition() {
        bandsDc.getSorter().sort(Sort.by(Sort.Direction.ASC, "position"));
    }

    @Subscribe("serviceTree.create")
    protected void onServiceTreeCreate(Action.ActionPerformedEvent event) {
        BandDefinition parentDefinition = bandsDc.getItem();
        Report report = reportDc.getItem();
        // Use root band as parent if no items selected
        if (parentDefinition == null) {
            parentDefinition = report.getRootBandDefinition();
        }
        if (parentDefinition.getChildrenBandDefinitions() == null) {
            parentDefinition.setChildrenBandDefinitions(new ArrayList<>());
        }


        orderBandDefinitions(parentDefinition);

        BandDefinition newBandDefinition = metadata.create(BandDefinition.class);
        newBandDefinition.setName("newBand" + (parentDefinition.getChildrenBandDefinitions().size() + 1));
        newBandDefinition.setOrientation(Orientation.HORIZONTAL);
        newBandDefinition.setParentBandDefinition(parentDefinition);
        if (parentDefinition.getChildrenBandDefinitions() != null) {
            newBandDefinition.setPosition(parentDefinition.getChildrenBandDefinitions().size());
        } else {
            newBandDefinition.setPosition(0);
        }
        newBandDefinition.setReport(report);
        parentDefinition.getChildrenBandDefinitions().add(newBandDefinition);

        bandsDc.getMutableItems().add(newBandDefinition);

        bandTree.expandTree();
        bandTree.setSelected(newBandDefinition);//let's try and see if it increases usability

        bandTree.focus();
    }

    @Install(to = "serviceTree.create", subject = "enabledRule")
    protected boolean serviceTreeCreateEnabledRule() {
        return isUpdatePermitted();
    }

    @Subscribe("serviceTree.remove")
    protected void onServiceTreeRemove(Action.ActionPerformedEvent event) {
        Set<BandDefinition> selected = bandTree.getSelected();
        removeChildrenCascade(selected);
        for (Object object : selected) {
            BandDefinition definition = (BandDefinition) object;
            if (definition.getParentBandDefinition() != null) {
                orderBandDefinitions(((BandDefinition) object).getParentBandDefinition());
            }
        }
        bandTree.focus();
    }

    @Install(to = "serviceTree.remove", subject = "enabledRule")
    protected boolean serviceTreeRemoveEnabledRule() {
        Object selectedItem = bandTree.getSingleSelected();
        if (selectedItem != null) {
            return !Objects.equals(reportDc.getItem().getRootBandDefinition(), selectedItem);
        }

        return false;
    }

    private void removeChildrenCascade(Collection selected) {
        for (Object o : selected) {
            BandDefinition definition = (BandDefinition) o;
            BandDefinition parentDefinition = definition.getParentBandDefinition();
            if (parentDefinition != null) {
                definition.getParentBandDefinition().getChildrenBandDefinitions().remove(definition);
            }

            if (definition.getChildrenBandDefinitions() != null) {
                removeChildrenCascade(new ArrayList<>(definition.getChildrenBandDefinitions()));
            }

            if (definition.getDataSets() != null) {
                bandsDc.setItem(definition);
                for (DataSet dataSet : new ArrayList<>(definition.getDataSets())) {
                    if (entityStates.isNew(dataSet)) {
                        dataSetsDc.getMutableItems().remove(dataSet);
                    }
                }
            }
            bandsDc.getMutableItems().remove(definition);
        }
    }

    protected void orderBandDefinitions(BandDefinition parent) {
        if (parent.getChildrenBandDefinitions() != null) {
            List<BandDefinition> childrenBandDefinitions = parent.getChildrenBandDefinitions();
            for (int i = 0, childrenBandDefinitionsSize = childrenBandDefinitions.size(); i < childrenBandDefinitionsSize; i++) {
                BandDefinition bandDefinition = childrenBandDefinitions.get(i);
                bandDefinition.setPosition(i);
            }
        }
    }

    @Subscribe("up")
    protected void onUpClick(Button.ClickEvent event) {
        BandDefinition definition = bandTree.getSingleSelected();
        if (definition != null && definition.getParentBandDefinition() != null) {
            BandDefinition parentDefinition = definition.getParentBandDefinition();
            List<BandDefinition> definitionsList = parentDefinition.getChildrenBandDefinitions();
            int index = definitionsList.indexOf(definition);
            if (index > 0) {
                BandDefinition previousDefinition = definitionsList.get(index - 1);
                definition.setPosition(definition.getPosition() - 1);
                previousDefinition.setPosition(previousDefinition.getPosition() + 1);

                definitionsList.set(index, previousDefinition);
                definitionsList.set(index - 1, definition);

                sortBandDefinitionsTableByPosition();
            }
        }
    }

    protected boolean isUpButtonEnabled() {
        if (bandTree != null) {
            BandDefinition selectedItem = bandTree.getSingleSelected();
            return selectedItem != null && selectedItem.getPosition() > 0 && isUpdatePermitted();
        }
        return false;
    }

    @Subscribe("down")
    protected void onDownClick(Button.ClickEvent event) {
        BandDefinition definition = bandTree.getSingleSelected();
        if (definition != null && definition.getParentBandDefinition() != null) {
            BandDefinition parentDefinition = definition.getParentBandDefinition();
            List<BandDefinition> definitionsList = parentDefinition.getChildrenBandDefinitions();
            int index = definitionsList.indexOf(definition);
            if (index < definitionsList.size() - 1) {
                BandDefinition nextDefinition = definitionsList.get(index + 1);
                definition.setPosition(definition.getPosition() + 1);
                nextDefinition.setPosition(nextDefinition.getPosition() - 1);

                definitionsList.set(index, nextDefinition);
                definitionsList.set(index + 1, definition);

                sortBandDefinitionsTableByPosition();
            }
        }
    }


    protected boolean isDownButtonEnabled() {
        if (bandTree != null) {
            BandDefinition bandDefinition = bandTree.getSingleSelected();
            if (bandDefinition != null) {
                BandDefinition parent = bandDefinition.getParentBandDefinition();
                return parent != null &&
                        parent.getChildrenBandDefinitions() != null &&
                        bandDefinition.getPosition() < parent.getChildrenBandDefinitions().size() - 1
                        && isUpdatePermitted();
            }
        }
        return false;
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected boolean isTemplateWithoutFile(ReportTemplate template) {
        return template.getOutputType() == JmixReportOutputType.chart ||
                template.getOutputType() == JmixReportOutputType.table ||
                template.getOutputType() == JmixReportOutputType.pivot;
    }

    protected boolean isChildOrEqual(BandDefinition definition, BandDefinition child) {
        if (definition.equals(child)) {
            return true;
        } else if (child != null) {
            return isChildOrEqual(definition, child.getParentBandDefinition());
        } else {
            return false;
        }
    }

    protected void setupDropZoneForTemplate() {
        final ReportTemplate defaultTemplate = reportDc.getItem().getDefaultTemplate();
        if (defaultTemplate != null) {
            invisibleFileUpload.setDropZone(new UploadField.DropZone(reportFields));
        } else {
            invisibleFileUpload.setDropZone(null);
        }
    }
}