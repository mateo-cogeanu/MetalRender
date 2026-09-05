import java.lang.reflect.Method;
import org.joml.Vector2i;
import org.joml.Vector3i;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vulkan.IrisVulkanUniformSnapshot;

/** Executed against the actual patched Iris dependency, without a game or GL context. */
class RendererRegression {
    public static void main(String[] args) throws Exception {
        Method type = CustomUniforms.class.getDeclaredMethod("glslType", kroppeb.stareval.function.Type.class);
        type.setAccessible(true);
        check("ivec2".equals(type.invoke(null, VectorType.I_VEC2)), "ivec2 mapping");
        check("ivec3".equals(type.invoke(null, VectorType.I_VEC3)), "ivec3 mapping");
        Vector2i eye = new Vector2i(32, 240);
        var eyeSnapshot = new CustomUniforms.Snapshot("ivec2", eye);
        eye.set(0);
        check(eyeSnapshot.value().equals(new Vector2i(32, 240)), "eye snapshot aliases live input");
        Vector3i position = new Vector3i(-1, 64, 23);
        var positionSnapshot = new CustomUniforms.Snapshot("ivec3", position);
        position.set(0);
        check(positionSnapshot.value().equals(new Vector3i(-1, 64, 23)), "position snapshot aliases live input");

        Method version = Class.forName("net.irisshaders.iris.vulkan.IrisVulkanShaderResources")
            .getDeclaredMethod("vulkanVersion", String.class);
        version.setAccessible(true);
        String source = "#version 330 core\nfloat iris_msl_new = 1.0; bool new = true; bool newer = new;\n";
        String translated = (String) version.invoke(null, source);
        check(translated.startsWith("#version 450 core\n"), "Vulkan GLSL version");
        check(translated.contains("bool iris_msl_new_ = true"), "MSL identifier collision");
        check(translated.contains("bool newer = iris_msl_new_;"), "identifier token boundaries");
        check(translated.contains("float iris_msl_new = 1.0"), "existing identifier preserved");

        check(((String) version.invoke(null, "#version 460 core\nvoid main() {}"))
            .startsWith("#version 460 core"), "newer GLSL version downgraded");

        int before = SystemTimeUniforms.COUNTER.getAsInt();
        IrisVulkanUniformSnapshot.beginFrame();
        check(SystemTimeUniforms.COUNTER.getAsInt() == (before + 1) % 720720, "custom clock not advanced");
        var snapshot = IrisVulkanUniformSnapshot.capture(java.util.List.of(
            new IrisVulkanUniformSnapshot.Field("frameCounter", "int"),
            new IrisVulkanUniformSnapshot.Field("frameTime", "float")));
        check(snapshot.data().getInt(0) == SystemTimeUniforms.COUNTER.getAsInt(), "clock disagreement");
        check(snapshot.data().getFloat(4) == SystemTimeUniforms.TIMER.getLastFrameTime(), "frame time disagreement");
        System.out.println("Renderer regression checks passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
