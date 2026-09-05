package com.pebbles_boon.metalrender.sodium.mixins;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class MetalRenderMixinPlugin implements IMixinConfigPlugin {
    private static final String IRIS_MIXIN_PREFIX =
            "com.pebbles_boon.metalrender.sodium.mixins.iris.";
    private static final String IRIS_VULKAN_BACKEND =
            "net/irisshaders/iris/vulkan/IrisNativeVulkan.class";

    private boolean irisHasNativeVulkanBackend;

    @Override
    public void onLoad(String mixinPackage) {
        irisHasNativeVulkanBackend = getClass().getClassLoader().getResource(IRIS_VULKAN_BACKEND) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!irisHasNativeVulkanBackend || !mixinClassName.startsWith(IRIS_MIXIN_PREFIX)) {
            return true;
        }

        // Iris' Vulkan fork still routes legacy texture tracking through this
        // interface. Keep the harmless sentinel implementation until that
        // tracker becomes backend-neutral upstream.
        return mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisGpuTextureMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisGlDebugMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisVanillaPipelineMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisRenderSystemMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisSamplerLimitsMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisButtonMixin")
                || mixinClassName.equals(IRIS_MIXIN_PREFIX + "IrisImageButtonMixin");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
