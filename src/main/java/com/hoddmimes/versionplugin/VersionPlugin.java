package com.hoddmimes.versionplugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public  abstract class VersionPlugin implements Plugin<Project>
{

    @Override
    public void apply(Project target) {
        target.getTasks().create("VersionTask", VersionTask.class);
    }
}
