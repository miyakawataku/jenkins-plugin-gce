package org.kink_lang.jenkins.plugins.gce;

import hudson.slaves.AbstractCloudComputer;

public class PersistentComputer extends AbstractCloudComputer<PersistentSlave> {

    public PersistentComputer(PersistentSlave slave) {
        super(slave);
    }

}

// vim: et sw=4 sts=4 fdm=marker
