package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudSlaveRetentionStrategy;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.AbstractCloudComputer;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;

public class PersistentSlave extends AbstractCloudSlave {

    private static final Logger LOGGER = Logger.getLogger(PersistentSlave.class.getName());

    /** Project of the slave. */
    private String project;

    /** Zone of the slave. */
    private String zone;

    @DataBoundConstructor
    public PersistentSlave(
            String project,
            String zone,
            String name,
            String desc,
            String numExecutors,
            String labelString) throws Descriptor.FormException, IOException{
        super(name, desc, "/tmp", numExecutors, Mode.NORMAL, labelString,
                new JNLPLauncher(),
                new CloudSlaveRetentionStrategy<Computer>(),
                Collections.<NodeProperty<PersistentSlave>>emptyList());
        this.project = project;
        this.zone = zone;
    }

    /**
     * Stores the project ID.
     */
    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Returns the project ID.
     */
    public String getProject() {
        return this.project;
    }

    /**
     * Stores the zone ID.
     */
    @DataBoundSetter
    public void setZone(String zone) {
        this.zone = zone;
    }

    /**
     * Returns the zone ID.
     */
    public String getZone() {
        return this.zone;
    }

    @Override
    public AbstractCloudComputer<PersistentSlave> createComputer() {
        return new PersistentComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        try {
            GceInstance gi = new GceInstance(project, zone, name);
            if (! gi.stop()) {
                LOGGER.info("failed to stop the instance");
            }
        } catch (GeneralSecurityException gsex) {
            throw new RuntimeException(gsex);
        }
    }

}

// vim: et sw=4 sts=4 fdm=marker
