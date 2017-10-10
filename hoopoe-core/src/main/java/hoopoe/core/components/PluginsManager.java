package hoopoe.core.components;

import com.google.common.math.LongMath;
import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.api.plugins.HoopoeMethodInfo;
import hoopoe.api.plugins.HoopoePlugin;
import hoopoe.core.HoopoeException;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.configuration.EnabledComponentData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginsManager {

    private static final int MAX_RECORDERS = 64;

    private static final long[] BIT_FLAGS = new long[MAX_RECORDERS];

    static {
        for (int i = 0; i < MAX_RECORDERS; i++) {
            BIT_FLAGS[i] = LongMath.pow(2, i);
        }
    }

    private final Collection<HoopoePlugin> plugins;
    private final List<HoopoeInvocationRecorder> recorders = new ArrayList<>(MAX_RECORDERS);

    public PluginsManager(Configuration configuration, ComponentLoader componentLoader) {
        this.plugins = new ArrayList<>();
        for (EnabledComponentData enabledPlugin : configuration.getEnabledPlugins()) {
            HoopoePlugin plugin = componentLoader.loadComponent(
                    enabledPlugin.getBinariesPath(), HoopoePlugin.class);
            configuration.setPluginConfiguration(plugin, enabledPlugin.getId());
            this.plugins.add(plugin);
        }
    }

    public Collection<HoopoeInvocationRecorder> getRecorders(long pluginRecordersReference) {
        Collection<HoopoeInvocationRecorder> referencedRecorders = new ArrayList<>();
        for (int i = 0; i < MAX_RECORDERS; i++) {
            long bitFlag = BIT_FLAGS[i];
            if ((pluginRecordersReference & bitFlag) == bitFlag) {
                referencedRecorders.add(recorders.get(i));
            }
        }
        return referencedRecorders;
    }

    public long getPluginRecordersReference(HoopoeMethodInfo methodInfo) {
        long recordersReference = 0;

        for (HoopoePlugin plugin : plugins) {
            HoopoeInvocationRecorder recorder = plugin.createRecorderIfSupported(methodInfo);
            if (recorder != null) {
                synchronized (recorders) {
                    int recordersCount = recorders.size();

                    if (recordersCount == MAX_RECORDERS) {
                        throw new HoopoeException("Maximum number of " + MAX_RECORDERS + " recorders is reached. "
                                + "Probably some of plugins are not optimized.");
                    }

                    int registeredRecorderIndex = recorders.indexOf(recorder);
                    if (registeredRecorderIndex < 0) {
                        registeredRecorderIndex = recordersCount;
                        recorders.add(registeredRecorderIndex, recorder);
                    }

                    recordersReference |= BIT_FLAGS[registeredRecorderIndex];
                }
            }
        }

        return recordersReference;
    }

}
