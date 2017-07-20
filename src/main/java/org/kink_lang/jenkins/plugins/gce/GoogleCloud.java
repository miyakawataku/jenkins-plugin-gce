package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class GoogleCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloud.class.getName());

    private String project;

    private String zone;

    private String credentialsFilePath;

    private List<PersistentSlaveSpec> persistentSlaveSpecs;

    @DataBoundConstructor
    public GoogleCloud(String name) {
        super(name);
    }

    /**
     * Sets the project ID of slaves.
     */
    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Returns the project ID of slaves.
     */
    public String getProject() {
        return this.project;
    }

    /**
     * Sets the zone ID of slaves.
     */
    @DataBoundSetter
    public void setZone(String zone) {
        this.zone = zone;
    }

    /**
     * Returns the zone ID of slaves.
     */
    public String getZone() {
        return this.zone;
    }

    @DataBoundSetter
    public void setPersistentSlaveSpecs(List<PersistentSlaveSpec> persistentSlaveSpecs) {
        this.persistentSlaveSpecs = persistentSlaveSpecs;
    }

    public List<PersistentSlaveSpec> getPersistentSlaveSpecs() {
        return this.persistentSlaveSpecs;
    }

    /**
     * Stores the path of the credentials file.
     */
    @DataBoundSetter
    public void setCredentialsFilePath(String credentialsFilePath) {
        this.credentialsFilePath = credentialsFilePath;
    }

    /**
     * Returns the path of the credentials file.
     */
    public String getCredentialsFilePath() {
        return this.credentialsFilePath;
    }

    @Override
    public boolean canProvision(Label label) {
        for (PersistentSlaveSpec spec : this.persistentSlaveSpecs) {
            if (spec.canProvision(label)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        List<NodeProvisioner.PlannedNode> result = new ArrayList<NodeProvisioner.PlannedNode>();
        for (PersistentSlaveSpec spec : this.persistentSlaveSpecs) {
            if (excessWorkload <= 0) {
                break;
            }

            NodeProvisioner.PlannedNode plannedNode = spec.provision(this, label, getProject(), getZone());
            if (plannedNode != null) {
                excessWorkload -= plannedNode.numExecutors;
                result.add(plannedNode);
            }
        }
        return result;
    }

    /**
     * Sets up and launches the instance.
     */
    public PersistentSlave setupAndLaunch(PersistentSlave slave) throws Exception {
        GceInstance gi = new GceInstance(project, zone, slave.getNodeName());
        Map<String, String> jenkinsMetadata = new HashMap<String, String>();
        jenkinsMetadata.put("jenkinsSecret", slave.getComputer().getJnlpMac());
        if (! gi.addMetadata(jenkinsMetadata)) {
            LOGGER.warning("provision: failed to add metadata for " + slave.getNodeName());
            return null;
        }

        if (! gi.start()) {
            LOGGER.warning("provision: failed to start " + slave.getNodeName());
            return null;
        }

        return slave;
    }

    /**
     * Terminates the instance.
     */
    public void terminate(String instanceName) throws IOException, InterruptedException {
        try {
            GceInstance gi = new GceInstance(project, zone, instanceName);
            if (! gi.stop()) {
                LOGGER.info("failed to stop the instance");
            }
        } catch (GeneralSecurityException gsex) {
            throw new RuntimeException(gsex);
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Google Compute Engine";
        }

        /**
         * Validates name property.
         */
        public FormValidation doCheckName(@QueryParameter String name) {
            return name.trim().isEmpty()
                ? FormValidation.error("Cloud name must be filled")
                : FormValidation.ok();
        }

        /**
         * Validates project property.
         */
        public FormValidation doCheckProject(@QueryParameter String project) {
            return project.trim().isEmpty()
                ? FormValidation.error("Project ID must be filled")
                : FormValidation.ok();
        }

        /**
         * Vaidates zone property.
         */
        public FormValidation doCheckZone(@QueryParameter String zone) {
            return zone.trim().isEmpty()
                ? FormValidation.error("Zone ID must be filled")
                : FormValidation.ok();
        }

    }

}

// vim: et sw=4 sts=4 fdm=marker
