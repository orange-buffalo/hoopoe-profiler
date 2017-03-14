package hoopoe.core;

import hoopoe.api.plugins.HoopoeInvocationRecorder;
import hoopoe.api.plugins.HoopoePlugin;
import hoopoe.core.configuration.Configuration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.AllArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.SerializedConstant;

public class PluginManager {

    private List<PluginActionWrapper> pluginActions = new CopyOnWriteArrayList<>();

    // todo multithreading
    // todo cleanup
    private Map<String, StackManipulation> pluginActionsCache = new HashMap<>();

    private ClassMetadataReader classMetadataReader;

    private Collection<HoopoePlugin> plugins;

    public PluginManager(Configuration configuration, ClassMetadataReader classMetadataReader) {

        this.classMetadataReader = classMetadataReader;

        //todo
        plugins = new ArrayList<>();
    }

    private int[] addPluginActions(HoopoeMethodInfoImpl methodInfo) {
        List<Integer> pluginActionIndicies = new ArrayList<>(0);
        for (HoopoePlugin plugin : plugins) {
            HoopoeInvocationRecorder pluginAction = plugin.createActionIfSupported(methodInfo);
            if (pluginAction != null) {
                pluginActions.add(new PluginActionWrapper(pluginAction, plugin));
                pluginActionIndicies.add(pluginActions.size() - 1);
            }
        }
//        return pluginActionIndicies.toArray(new Integer[pluginActionIndicies.size()]);
        //todo
        return null;
    }

    public Collection<HoopoeInvocationRecorder> getRecorders(Object pluginActionIndicies) {
        Collection<HoopoeInvocationRecorder> actions = new ArrayList<>();
        for (int pluginActionIndex : (int[]) pluginActionIndicies) {
            PluginActionWrapper pluginActionWrapper = pluginActions.get(pluginActionIndex);
            actions.add(pluginActionWrapper.pluginAction);
        }
        return actions;
    }

    public StackManipulation getPluginActions(MethodDescription method) {
        TypeDefinition declaringType = method.getDeclaringType();
        String className = classMetadataReader.getClassName(declaringType);
        String methodSignature = classMetadataReader.getMethodSignature(method);

        String methodKey = className + methodSignature;
        if (pluginActionsCache.containsKey(methodKey)) {
            return pluginActionsCache.get(methodKey);
        }

        HoopoeMethodInfoImpl methodInfo = new HoopoeMethodInfoImpl(
                className,
                methodSignature,
                classMetadataReader.getSuperClassesNames(declaringType));

        int[] rawPluginActionIndicies = addPluginActions(methodInfo);
        StackManipulation stackManipulation = SerializedConstant.of(rawPluginActionIndicies);
        pluginActionsCache.put(methodKey, stackManipulation);
        return stackManipulation;
    }

    @AllArgsConstructor
    private static class PluginActionWrapper {
        HoopoeInvocationRecorder pluginAction;
        HoopoePlugin plugin;
    }

}
