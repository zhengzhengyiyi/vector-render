package net.zhengzhengyiyi.renderer.gl;

import net.zhengzhengyiyi.renderer.api.blaze3d.opengl.GlStateManager;
import net.zhengzhengyiyi.renderer.api.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CompiledShader implements AutoCloseable {
   public static final CompiledShader INVALID_SHADER = new CompiledShader(-1, new Identifier("minecraft", "invalid"), ShaderType.VERTEX);
   private final Identifier id;
   private int handle;
   private final ShaderType shaderType;

   public CompiledShader(int handle, Identifier id, ShaderType shaderType) {
      this.id = id;
      this.handle = handle;
      this.shaderType = shaderType;
   }

   @Override
   public void close() {
      if (this.handle == -1) {
         throw new IllegalStateException("Already closed");
      } else {
         RenderSystem.assertOnRenderThread();
         GlStateManager.glDeleteShader(this.handle);
         this.handle = -1;
      }
   }

   public Identifier getId() {
      return this.id;
   }

   public int getHandle() {
      return this.handle;
   }

   public String getDebugLabel() {
      return this.shaderType.idConverter().toResourcePath(this.id).toString();
   }
}
