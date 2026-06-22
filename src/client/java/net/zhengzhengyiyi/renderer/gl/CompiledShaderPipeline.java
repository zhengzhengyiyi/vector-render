package net.zhengzhengyiyi.renderer.gl;

import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.CompiledRenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.RenderPipeline;
import net.zhengzhengyiyi.renderer.v211.gl.ShaderProgram;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record CompiledShaderPipeline(RenderPipeline info, ShaderProgram program) implements CompiledRenderPipeline {
   @Override
   public boolean isValid() {
      return this.program != ShaderProgram.INVALID;
   }
}
