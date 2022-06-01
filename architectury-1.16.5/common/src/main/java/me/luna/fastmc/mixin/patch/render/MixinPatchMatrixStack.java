package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedMatrix3f;
import me.luna.fastmc.mixin.IPatchedMatrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(MatrixStack.class)
public class MixinPatchMatrixStack {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;
    private final ArrayDeque<MatrixStack.Entry> pool = new ArrayDeque<>();

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public void translate(double x, double y, double z) {
        MatrixStack.Entry entry = this.stack.getLast();
        ((IPatchedMatrix4f) (Object) entry.getModel()).translate((float) x, (float) y, (float) z);
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public void scale(float x, float y, float z) {
        MatrixStack.Entry entry = this.stack.getLast();
        ((IPatchedMatrix4f) (Object) entry.getModel()).scale(x, y, z);

        if (x == y && y == z) {
            if (x > 0.0F) {
                return;
            }
            entry.getNormal().multiply(-1.0F);
        }

        x = 1.0f / x;
        y = 1.0f / y;
        z = 1.0f / z;
        float i = MathHelper.fastInverseCbrt(x * y * z);
        ((IPatchedMatrix3f) (Object) entry.getNormal()).scale(i * x, i * y, i * z);
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public void push() {
        MatrixStack.Entry entry = this.stack.getLast();
        MatrixStack.Entry newEntry = this.pool.pollLast();
        if (newEntry == null) {
            newEntry = new MatrixStack.Entry(new Matrix4f(), new Matrix3f());
        }
        ((IPatchedMatrix4f) (Object) newEntry.getModel()).set(entry.getModel());
        ((IPatchedMatrix3f) (Object) newEntry.getNormal()).set(entry.getNormal());
        this.stack.addLast(newEntry);
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public void pop() {
        MatrixStack.Entry entry = this.stack.removeLast();
        pool.addLast(entry);
    }
}
