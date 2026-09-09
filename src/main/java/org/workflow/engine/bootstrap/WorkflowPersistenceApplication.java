package org.workflow.engine.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPersistenceApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowPersistenceApplication.class);

    public static void main(String[] args) {
        LOGGER.info("=====================================");
        LOGGER.info("Workflow Persistence Engine Started");
        LOGGER.info("Java Runtime : {}", Runtime.version());
        LOGGER.info("=====================================");
    }

}
