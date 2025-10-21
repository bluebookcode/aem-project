package com.aem.project.core.servlets;

import com.day.cq.dam.api.AssetReferenceResolver;
import com.day.cq.wcm.command.api.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
        service = { Servlet.class },
        property = {
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/assetOperation",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET"
        }
)
public class AssetMoveCBMethod extends SlingSafeMethodsServlet {

    private static final Logger logger = LoggerFactory.getLogger(AssetMoveCBMethod.class);

    @Reference
    private CommandBuilderFactory commandBuilderFactory;

    @Reference
    private AssetReferenceResolver assetReferenceResolver;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        String operation = request.getParameter("operation");  // move | copy | rename
        String srcPath = request.getParameter("src");
        String dstPath = request.getParameter("dst");

        response.setContentType("application/json");

        if (srcPath == null || dstPath == null || operation == null) {
            response.getWriter().write("{\"error\":\"Missing required parameters: src, dst, or operation\"}");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();

        try {
            boolean isCopy = operation.equalsIgnoreCase("copy");
            boolean isMove = operation.equalsIgnoreCase("move") || operation.equalsIgnoreCase("rename");

            CopyMoveCommandBuilder copyMoveCommandBuilder = commandBuilderFactory.createCommandBuilder(CopyMoveCommandBuilder.class)
                    .withCopy(isCopy)
                    .withResourceResolver(resolver)
                    .withAssetReferenceResolver(assetReferenceResolver)
                    .withRetrieveAllRefs(true)
                    .withShallow(false)
                    .withCheckIntegrity(true);

            CopyMoveCommandPathArgument copyMoveCmdPathArg =
                    (CopyMoveCommandPathArgument) copyMoveCommandBuilder.createPathArgumentBuilder()
                            .withSrcPath(srcPath)
                            .withDstPath(dstPath)
                            .withAdjustRefPaths(null)
                            .build();

            copyMoveCommandBuilder.withPathArgument(copyMoveCmdPathArg);

            Command cmd = copyMoveCommandBuilder.build();
            CopyMoveCommandResult result = (CopyMoveCommandResult) cmd.execute();

            resolver.commit();

            if (result.executionSucceeded()) {
                response.getWriter().write(String.format(
                        "{\"message\":\"Asset %s successfully from %s to %s\"}",
                        operation, srcPath, dstPath
                ));
            } else {
                response.getWriter().write(String.format(
                        "{\"error\":\"Failed to %s asset. Destination paths: %s\"}",
                        operation, result.getDestinationPaths()
                ));
            }

        } catch (Exception e) {
            logger.error("Error during asset operation", e);
            response.getWriter().write("{\"exception\":\"" + e.getMessage() + "\"}");
        }
    }
}
