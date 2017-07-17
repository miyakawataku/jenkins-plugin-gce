package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Metadata;
import com.google.api.services.compute.model.Operation;

public class GceInstance {

    private static final Logger LOGGER = Logger.getLogger(GceInstance.class.getName());

    private final String project;

    private final String zone;

    private final String name;

    private final HttpTransport transport;

    private final JsonFactory jsonFactory;

    private final GoogleCredential credential;

    private final Compute compute;

    public GceInstance(String project, String zone, String name) throws IOException, GeneralSecurityException {
        this.project = project;
        this.zone = zone;
        this.name = name;
        this.transport = GoogleNetHttpTransport.newTrustedTransport();
        this.jsonFactory = new JacksonFactory();
        this.credential = GoogleCredential.getApplicationDefault();
        this.compute = new Compute.Builder(transport, jsonFactory, credential).build();
    }

    public boolean addMetadata(Map<String, String> addedMetadata) throws Exception {
        Instance instance = compute.instances().get(project, zone, name).execute();
        Metadata metadata = updateMetadata(instance.getMetadata(), addedMetadata);
        Operation op = compute.instances()
            .setMetadata(project, zone, name, metadata)
            .execute();
        return waitOperationResult(op);
    }

    private Metadata updateMetadata(Metadata metadata, Map<String, String> added) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        for (Metadata.Items item : metadata.getItems()) {
            map.put(item.getKey(), item.getValue());
        }
        map.putAll(added);

        List<Metadata.Items> items = new ArrayList<Metadata.Items>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            items.add(new Metadata.Items().setKey(key).setValue(val));
        }
        metadata.setItems(items);
        return metadata;
    }

    public boolean start() throws Exception {
        Operation op = compute.instances()
            .start(project, zone, name)
            .execute();
        return waitOperationResult(op);
    }

    boolean waitOperationResult(Operation op) throws Exception {
        while (true) {
            LOGGER.info("Operation result: " + op);
            if (op.getStatus().equals("DONE")) {
                return op.getError() == null
                    || op.getError().getErrors() == null
                    || op.getError().getErrors().isEmpty();
            }
            Thread.sleep(3000);

            String selfLink = op.getSelfLink();
            HttpResponse selfResp = transport.createRequestFactory(credential)
                .buildRequest("GET", new GenericUrl(selfLink), null)
                .execute();
            op = jsonFactory.fromInputStream(selfResp.getContent(),
                    StandardCharsets.UTF_8,
                    Operation.class);
        }
    }

}

// vim: et sw=4 sts=4 fdm=marker
