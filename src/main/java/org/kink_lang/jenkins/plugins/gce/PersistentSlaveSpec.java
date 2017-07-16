package org.kink_lang.jenkins.plugins.gce;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.tools.ToolLocationNodeProperty;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

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
