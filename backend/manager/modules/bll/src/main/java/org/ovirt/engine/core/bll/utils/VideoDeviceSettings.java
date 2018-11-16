package org.ovirt.engine.core.bll.utils;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsProperties;

@Singleton
public class VideoDeviceSettings {
    @Inject
    private VgamemVideoSettings vgamemVideoSettings;

    private Map<String, Integer> getVideoDeviceSettings(VmBase vmBase) {
        if (vmBase.getDefaultDisplayType() == DisplayType.qxl) {
            return vgamemVideoSettings.getQxlVideoDeviceSettings(vmBase);
        } else {
            return vgamemVideoSettings.getVgaVideoDeviceSettings();
        }
    }

    /**
     * Returns video device spec params.
     *
     * @return a map of device parameters
     */
    public Map<String, Object> getVideoDeviceSpecParams(VmBase vmBase) {
        return getVideoDeviceSettings(vmBase).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    /**
     * Returns total video RAM size to be allocated for the given VM.
     *
     * @param vm the VM to compute the video RAM size for
     * @return size of the video RAM in MiB
     */
    public int totalVideoRAMSizeMb(VM vm) {
        Map<String, Integer> settings = getVideoDeviceSettings(vm.getStaticData());
        return (settings.getOrDefault(VdsProperties.VIDEO_RAM, 0) +
                settings.getOrDefault(VdsProperties.VIDEO_VRAM, 0) + 1023) / 1024;
    }

}
