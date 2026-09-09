// Introduces logging
// SLF4J library - Simple Logging Facade for Java

package org.workflow.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Workflow Persistence Engine Started!");
    }
}