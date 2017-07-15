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

public class PersistentSshSlaveSpec
    extends AbstractDescribableImpl<PersistentSshSlaveSpec> implements Serializable {

    private String instanceName;

    private String label;

    @DataBoundConstructor
    public PersistentSshSlaveSpec() {
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

    @Extension
    public static final class DescriptorImpl extends Descriptor<PersistentSshSlaveSpec> {

        @Override
        public String getDisplayName() {
            return "Persistent SSH Slave";
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

    }

}

// vim: et sw=4 sts=4 fdm=marker
