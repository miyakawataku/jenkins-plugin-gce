package org.kink_lang.jenkins.plugins.gce;

import java.io.Serializable;
import java.util.List;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.tools.ToolLocationNodeProperty;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class PersistentSshSlaveSpec
    extends AbstractDescribableImpl<PersistentSshSlaveSpec> implements Serializable {

    private String name;

    private transient List<ToolLocationNodeProperty> nodeProperties;

    @DataBoundConstructor
    public PersistentSshSlaveSpec() {
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    @DataBoundSetter
    public void setNodeProperties(List<ToolLocationNodeProperty> nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PersistentSshSlaveSpec> {

        @Override
        public String getDisplayName() {
            return "Persistent SSH Slave";
        }

    }

}

// vim: et sw=4 sts=4 fdm=marker
