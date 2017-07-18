package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

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

public class PersistentSlaveSpec
    extends AbstractDescribableImpl<PersistentSlaveSpec>
    implements Serializable {

    private static Logger LOGGER = Logger.getLogger(PersistentSlaveSpec.class.getName());

    private String instanceName;

    private String label;

    /** Seconds for which the slave stays up. */
    private String retentionSeconds;

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
        return this.instanceName.trim();
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
     * Stores seconds for which the slave stays up.
     */
    @DataBoundSetter
    public void setRetentionSeconds(String retentionSeconds) {
        this.retentionSeconds = retentionSeconds;
    }

    /**
     * Returns the seconds for which the slave stays up.
     */
    public String getRetentionSeconds() {
        return this.retentionSeconds;
    }

    /**
     * Returns true if the slave can be provisioned.
     */
    public boolean canProvision(Label label) {
        boolean result = label.matches(getLabelAtoms())
            && Jenkins.getInstance().getNodesObject().getNode(this.getInstanceName()) == null;
        LOGGER.info("canProvision => " + result);
        return result;
    }

    /**
     * Provisions the specified instance.
     */
    public NodeProvisioner.PlannedNode provision(Label label, final String project, final String zone) {
        if (! canProvision(label)) {
            LOGGER.info("provision: cannot provision");
            return null;
        }

        LOGGER.info("provision: try provisioning!");
        try {
            final PersistentSlave slave = new PersistentSlave(
                    getInstanceName(),
                    this.nodeDescription,
                    this.numExecutors,
                    this.label,
                    getRetentionSeconds());
            slave.setProject(project);
            slave.setZone(zone);
            Jenkins.getInstance().addNode(slave);

            Future<Node> future = Computer.threadPoolForRemoting.submit(new Callable<Node>() {
                @Override public Node call() throws Exception {
                    return setupAndLaunch(project, zone, slave);
                }
            });
            return new NodeProvisioner.PlannedNode(getInstanceName(), future, slave.getComputer().getNumExecutors());
        } catch (IOException ioex) {
            LOGGER.log(Level.WARNING, "provision: failed provisioning of " + getInstanceName(), ioex);
            return null;
        } catch (Descriptor.FormException formex) {
            LOGGER.log(Level.WARNING, "provision: failed provisioning of " + getInstanceName(), formex);
            return null;
        }
    }

    /**
     * Sets up and launches the instance.
     */
    private Node setupAndLaunch(String project, String zone, PersistentSlave slave) throws Exception {
        GceInstance gi = new GceInstance(project, zone, instanceName);
        Map<String, String> jenkinsMetadata = new HashMap<String, String>();
        jenkinsMetadata.put("jenkinsSecret", slave.getComputer().getJnlpMac());
        if (! gi.addMetadata(jenkinsMetadata)) {
            LOGGER.warning("provision: failed to add metadata for " + getInstanceName());
            return null;
        }

        if (! gi.start()) {
            LOGGER.warning("provision: failed to start " + getInstanceName());
            return null;
        }

        return slave;
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
