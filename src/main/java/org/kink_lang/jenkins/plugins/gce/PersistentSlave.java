package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.Cloud;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;

public class PersistentSlave extends AbstractCloudSlave {

    private static final Logger LOGGER = Logger.getLogger(PersistentSlave.class.getName());

    /** Cloud name. */
    private String cloudName;

    /** Seconds for which the slave stays up when idle. */
    private String retentionSeconds;

    @DataBoundConstructor
    public PersistentSlave(
            String name,
            String nodeDescription,
            String remoteFS,
            String numExecutors,
            String labelString,
            String retentionSeconds) throws Descriptor.FormException, IOException{
        super(name, nodeDescription, remoteFS, numExecutors, Mode.NORMAL, labelString,
                new JNLPLauncher(),
                new CloudRetentionStrategy(Integer.parseInt(retentionSeconds)),
                Collections.<NodeProperty<PersistentSlave>>emptyList());
        this.retentionSeconds = retentionSeconds;
    }

    /**
     * Stores the cloud name.
     */
    @DataBoundSetter
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    /**
     * Returns the cloud name.
     */
    public String getCloudName() {
        return this.cloudName;
    }

    /**
     * Stores the retentionSeconds property.
     */
    public void setRetentionSeconds(String retentionSeconds) {
        this.retentionSeconds = retentionSeconds;
        this.setRetentionStrategy(new CloudRetentionStrategy(Integer.parseInt(retentionSeconds)));
    }

    /**
     * Returns the retentionSeconds property.
     */
    public String getRetentionSeconds() {
        return this.retentionSeconds;
    }

    @Override
    public AbstractCloudComputer<PersistentSlave> createComputer() {
        return new PersistentComputer(this);
    }

    /**
     * Returns the cloud which makes the slave.
     */
    private GoogleCloud getCloud() {
        Cloud cloud = Jenkins.getInstance().getCloud(cloudName);
        if (cloud == null) {
            String msg = String.format(
                    "cannot terminate the slave instance: cloud %s not found", cloudName);
            throw new RuntimeException(msg);
        }

        if (! (cloud instanceof GoogleCloud)) {
            String msg = String.format(
                    "cannot terminate the slave instance: cloud %s is not a GoogleCloud", cloudName);
            throw new RuntimeException(msg);
        }

        return (GoogleCloud) cloud;
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        GoogleCloud cloud = getCloud();
        cloud.terminate(this.name);
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }

        /** Pattern of decimal numbers. */
        private static final Pattern DECIMAL_PATTERN = Pattern.compile("[0-9]+");

        /**
         * Checks retentionSeconds field.
         */
        public FormValidation doCheckRetentionSeconds(@QueryParameter String retentionSeconds) {
            return DECIMAL_PATTERN.matcher(retentionSeconds).matches()
                ? FormValidation.ok()
                : FormValidation.error("Retention seconds must be a decimal number");

        }

    }

}

// vim: et sw=4 sts=4 fdm=marker
