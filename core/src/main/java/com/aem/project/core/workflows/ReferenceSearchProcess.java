package com.aem.project.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.wcm.commons.ReferenceSearch;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(
        service = WorkflowProcess.class,
        property = {
                "process.label=Search Payload References Process Step"
        }
)
public class ReferenceSearchProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(ReferenceSearchProcess.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        try {
            // Get the workflow payload (usually the resource path)
            String payloadPath = workItem.getWorkflowData().getPayload().toString();
            log.info("Payload Path: {}", payloadPath);

            // Get resolver from the workflow session
            ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);

            if (resolver != null) {
                ReferenceSearch referenceSearch = new ReferenceSearch();
                Map<String, ReferenceSearch.Info> references = referenceSearch.search(resolver, payloadPath);

                if (references != null && !references.isEmpty()) {
                    for (Map.Entry<String, ReferenceSearch.Info> entry : references.entrySet()) {
                        log.info("Reference found on page: {}", entry.getKey());

                        ReferenceSearch.Info info = entry.getValue();
                        if (info != null && info.getProperties() != null) {
                            for (String property : info.getProperties()) {
                                log.info("Referenced property: {}", property);
                            }
                        }
                    }
                } else {
                    log.info("No references found for payload: {}", payloadPath);
                }
            } else {
                log.error("ResourceResolver is null, cannot proceed with ReferenceSearch.");
            }

        } catch (Exception e) {
            log.error("Error while searching references for payload: ", e);
            throw new WorkflowException(e);
        }
    }
}

