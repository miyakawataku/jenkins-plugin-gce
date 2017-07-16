package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.NodeProvisioner;
import hudson.tools.ToolLocationNodeProperty;
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
import com.google.api.services.compute.model.Metadata;

public class PersistentSlaveSpec
    extends AbstractDescribableImpl<PersistentSlaveSpec>
    implements Serializable {

    private String instanceName;

    private String label;

    /** # of executors. */
    private String numExecutors;

    /** The description of the node. */
    private String nodeDescription;

    @DataBoundConstructor
    public PersistentSlaveSpec() {
    }

    @DataBoundSetter
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public Set<LabelAtom> getLabelAtoms() {
        return Label.parse(this.label);
    }

    /**
     * Stores # of executors.
     */
    @DataBoundSetter
    public void setNumExecutors(String numExecutors) {
        this.numExecutors = numExecutors;
    }

    /**
     * Returns # of executors.
     */
    public String getNumExecutors() {
        return this.numExecutors;
    }

    /**
     * Stores the description of the node.
     */
    @DataBoundSetter
    public void setNodeDescription(String nodeDescription) {
        this.nodeDescription = nodeDescription;
    }

    /**
     * Returns the description of the node.
     */
    public String getNodeDescription() {
        return this.nodeDescription;
    }

    /**
     * Returns true if the slave can be provisioned.
     */
    public boolean canProvision(Label label) {
        return label.matches(getLabelAtoms())
            && Jenkins.getInstance().getNodesObject().getNode(this.instanceName) == null;
    }

    public NodeProvisioner.PlannedNode provision(Label label, String project, String zone) {
        if (! canProvision(label)) {
            return null;
        }

        PersistentSlave slave ;
        try {
            slave = new PersistentSlave(
                    project, zone, this.instanceName,
                    this.nodeDescription,
                    this.numExecutors,
                    this.label);
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        } catch (Descriptor.FormException formEx) {
            throw new RuntimeException(formEx);
        }
        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        String jenkinsUrl = locationConfiguration.getUrl();
        String jnlpUrl = jenkinsUrl + "/" + slave.getComputer().getUrl() + "slave-agent.jnlp";
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            GoogleCredential credential = GoogleCredential.getApplicationDefault();
            Compute compute = new Compute.Builder(transport, jsonFactory, credential).build();
            Metadata metadata = new Metadata();
            metadata.setItems(Arrays.asList(
                        new Metadata.Items().set("jenkinsSecret", slave.getComputer().getJnlpMac()),
                        new Metadata.Items().set("jenkinsJnlpUrl", jnlpUrl)));
            compute.instances().setMetadata(project, zone, this.instanceName, metadata).execute();
            compute.instances().start(project, zone, this.instanceName).execute();
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        } catch (GeneralSecurityException gsex) {
            throw new RuntimeException(gsex);
        }
        final Node node = slave;
        Future<Node> future = Computer.threadPoolForRemoting.submit(new Callable<Node>() {
            @Override public Node call() throws Exception { return node; }
        });
        return new NodeProvisioner.PlannedNode(this.instanceName, future, slave.getComputer().getNumExecutors());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PersistentSlaveSpec> {

        @Override
        public String getDisplayName() {
            return "Persistent Slave";
        }

        /**
         * Checks instanceName field.
         *
         * See https://wiki.jenkins.io/display/JENKINS/Form+Validation .
         */
        public FormValidation doCheckInstanceName(@QueryParameter String instanceName) {
            return instanceName.trim().isEmpty()
                ? FormValidation.error("Instance name must be filled")
                : FormValidation.ok();
        }

        /**
         * Checks # of executors.
         */
        public FormValidation doCheckNumExecutors(@QueryParameter String numExecutors) {
            return numExecutors.matches("[0-9]+")
                ? FormValidation.ok()
                : FormValidation.error("# of executors must be an integer");
        }

    }

}

// vim: et sw=4 sts=4 fdm=marker
