# GCE Jenkins Plugin

## Build

Run the command bellow:

    mvn package

## Install

Install target/jenkins-plugin-gce.hpi to your Jenkins instance as a plugin.

## Usage

Jenkins setting can be done as below.

1.  Go to “Configure System” page.
2.  Press “Add” drop down button on “Clouds” section, then select “Google Compute Engine”
3.  Configure slave nodes

Slave nodes are configured as JNLP slaves.
JNLP secret is stored as “jenkinsSecret” attribute in the instance metadata.
Hence, VMs must execute slave.jar to connect to master on startup.

One way to do that is placing the shell script below on the slave VM
and configure it as a systemd service.

    #!/bin/bash

    while true; do
      jenkinsSecret=`curl --fail -H 'Metadata-Flavor: Google' 'http://metadata/computeMetadata/v1/instance/attributes/jenkinsSecret'`
      if [ $? -eq 0 ]; then
        break
      fi
      sleep 5
    done

    master_host={{MASTER_HOST}}
    master_port={{MASTER_PORT}}
    master_root_url=http://${master_host}:${master_port}
    slave_host=`hostname`

    wget -O slave.jar "${master_root_url}/jnlpJars/slave.jar"

    java -jar slave.jar \
      -jnlpUrl "${master_root_url}/computer/${slave_host}/slave-agent.jnlp" \
      -secret "${jenkinsSecret}"

## License

GCE Jenkins Plugin is distributed by MIT license.
See LICENSE.txt.

<!-- vim: set et sw=2 sts=2 : -->
