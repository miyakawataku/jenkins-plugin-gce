package org.kink_lang.jenkins.plugins.gce;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private List<? extends PersistentSshSlaveSpec> persistentSshSlaveSpecs = new ArrayList<>();

    @DataBoundConstructor
    public GoogleCloud(String name) {
        super(name);
    }

    @DataBoundSetter
    public void setPersistentSshSlaveSpecs(List<? extends PersistentSshSlaveSpec> persistentSshSlaveSpecs) {
        this.persistentSshSlaveSpecs = persistentSshSlaveSpecs;
    }

    public List<? extends PersistentSshSlaveSpec> getPersistentSshSlaveSpecs() {
        return this.persistentSshSlaveSpecs;
    }

    @Override
    public boolean canProvision(Label label) {
        return true;
    }

    @Override
    public List<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        throw new UnsupportedOperationException();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Google Compute Engine";
        }

        public FormValidation doCheckName(@QueryParameter String name) {
            return name.trim().isEmpty()
                ? FormValidation.error("Cloud name must be filled")
                : FormValidation.ok();
        }

    }

}

// vim: et sw=4 sts=4 fdm=marker
