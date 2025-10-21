package com.aem.project.core.servlets;

import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.wcm.commons.ReferenceSearch;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/assetmove"
        }
)
public class AssetMoveServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(AssetMoveServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        ResourceResolver resolver = request.getResourceResolver();
        String source = request.getParameter("source");
        String destination = request.getParameter("destination");

        JSONObject jsonResponse = new JSONObject();

        try {
            if (source == null || destination == null) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                jsonResponse.put("error", "Source and destination paths are required parameters.");
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            log.info("Moving asset from {} to {}", source, destination);

            // Step 1: Move asset using AssetManager
            AssetManager assetManager = resolver.adaptTo(AssetManager.class);
            if (assetManager == null) {
                throw new IllegalStateException("Cannot adapt to AssetManager!");
            }
            assetManager.moveAsset(source, destination);
            resolver.commit();
            log.info("Asset moved successfully from {} to {}", source, destination);

            // Step 2: Find references using ReferenceSearch
            ReferenceSearch referenceSearch = new ReferenceSearch();
            Map<String, ReferenceSearch.Info> references = referenceSearch.search(resolver, source);

            if (references != null && !references.isEmpty()) {
                log.info("Found {} references for asset {}", references.size(), source);

                // Extract reference paths as String array
                String[] refPaths = references.keySet().toArray(new String[0]);

                // Use adjustReferences to update all references from source to destination
                Collection<String> updatedRefs = referenceSearch.adjustReferences(resolver, source, destination, refPaths);
                resolver.commit();

                log.info("References updated using adjustReferences method for {} paths.", updatedRefs.size());
                jsonResponse.put("status", "Asset moved and references updated.");
                jsonResponse.put("updatedReferencesCount", updatedRefs.size());
            } else {
                log.info("No references found for asset {}", source);
                jsonResponse.put("status", "Asset moved, no references found.");
            }


        } catch (Exception e) {
            log.error("Error moving asset or updating references: ", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                jsonResponse.put("error", e.getMessage());
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        }

        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }
}
