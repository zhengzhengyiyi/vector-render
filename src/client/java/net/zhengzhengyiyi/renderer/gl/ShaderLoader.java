package net.zhengzhengyiyi.renderer.gl;
import net.zhengzhengyiyi.client.render.RenderEngine;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.CompiledRenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.pipeline.RenderPipeline;
import net.zhengzhengyiyi.renderer.api.blaze3d.shaders.ShaderType;
import net.zhengzhengyiyi.renderer.api.blaze3d.systems.GpuDevice;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.zhengzhengyiyi.renderer.v211.render.ProjectionMatrix2;
import net.minecraft.client.gl.GlImportProcessor;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ShaderLoader extends SinglePreparationResourceReloader<ShaderLoader.Definitions> implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final int field_53936 = 32768;
   public static final String SHADERS_PATH = "shaders";
   private static final ResourceFinder POST_EFFECT_FINDER = ResourceFinder.json("post_effect");
   final TextureManager textureManager;
   private final Consumer<Exception> onError;
   private ShaderLoader.Cache cache = new ShaderLoader.Cache(ShaderLoader.Definitions.EMPTY);
   final ProjectionMatrix2 projectionMatrix = new ProjectionMatrix2("post", 0.1F, 1000.0F, false);

   public ShaderLoader(TextureManager textureManager, Consumer<Exception> onError) {
      this.textureManager = textureManager;
      this.onError = onError;
   }

   protected ShaderLoader.Definitions prepare(ResourceManager resourceManager, Profiler profiler) {
      Builder<ShaderLoader.ShaderSourceKey, String> builder = ImmutableMap.builder();
      Map<Identifier, Resource> map = resourceManager.findResources("shaders", ShaderLoader::isShaderSource);

      for (Entry<Identifier, Resource> entry : map.entrySet()) {
         Identifier identifier = entry.getKey();
         ShaderType shaderType = ShaderType.byLocation(identifier);
         if (shaderType != null) {
            loadShaderSource(identifier, entry.getValue(), shaderType, map, builder);
         }
      }

      Builder<Identifier, PostEffectPipeline> builder2 = ImmutableMap.builder();

      for (Entry<Identifier, Resource> entry2 : POST_EFFECT_FINDER.findResources(resourceManager).entrySet()) {
         loadPostEffect(entry2.getKey(), entry2.getValue(), builder2);
      }

      return new ShaderLoader.Definitions(builder.build(), builder2.build());
   }

   private static void loadShaderSource(
      Identifier id, Resource resource, ShaderType type, Map<Identifier, Resource> allResources, Builder<ShaderLoader.ShaderSourceKey, String> builder
   ) {
      Identifier identifier = type.idConverter().toResourceId(id);
      GlImportProcessor glImportProcessor = createImportProcessor(allResources, id);

      try (Reader reader = resource.getReader()) {
         String string = IOUtils.toString(reader);
         builder.put(new ShaderLoader.ShaderSourceKey(identifier, type), String.join("", glImportProcessor.readSource(string)));
      } catch (IOException var12) {
         LOGGER.error("Failed to load shader source at {}", id, var12);
      }
   }

   private static GlImportProcessor createImportProcessor(Map<Identifier, Resource> allResources, Identifier id) {
      final Identifier identifier = id;
      return new GlImportProcessor() {
         private final Set<Identifier> processed = new ObjectArraySet<>();

         @Nullable
         @Override
         public String loadImport(boolean inline, String name) {
            Identifier identifierx;
            try {
               if (inline) {
                  identifierx = identifier.withPath(path -> path + name);
               } else {
                  identifierx = new Identifier("minecraft", "shaders/include/" + name);
               }
            } catch (InvalidIdentifierException var8) {
               ShaderLoader.LOGGER.error("Malformed GLSL import {}: {}", name, var8.getMessage());
               return "#error " + var8.getMessage();
            }

            if (!this.processed.add(identifierx)) {
               return null;
            } else {
               try {
                  String var5;
                  try (Reader reader = allResources.get(identifierx).getReader()) {
                     var5 = IOUtils.toString(reader);
                  }

                  return var5;
               } catch (IOException var10) {
                  ShaderLoader.LOGGER.error("Could not open GLSL import {}: {}", identifierx, var10.getMessage());
                  return "#error " + var10.getMessage();
               }
            }
         }
      };
   }

   private static void loadPostEffect(Identifier id, Resource resource, Builder<Identifier, PostEffectPipeline> builder) {
      Identifier identifier = POST_EFFECT_FINDER.toResourceId(id);

      try (Reader reader = resource.getReader()) {
         JsonElement jsonElement = JsonParser.parseReader(reader);
		builder.put(identifier, PostEffectPipeline.CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElseThrow());
      } catch (JsonParseException | IOException var9) {
         LOGGER.error("Failed to parse post chain at {}", id, var9);
      }
   }

   private static boolean isShaderSource(Identifier id) {
      return ShaderType.byLocation(id) != null || id.getPath().endsWith(".glsl");
   }

   protected void apply(ShaderLoader.Definitions definitions, ResourceManager resourceManager, Profiler profiler) {
      ShaderLoader.Cache cache = new ShaderLoader.Cache(definitions);
      Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getAll());
      List<Identifier> list = new ArrayList<>();
      GpuDevice gpuDevice = RenderEngine.getDevice();
      gpuDevice.clearPipelineCache();

      for (RenderPipeline renderPipeline : set) {
         CompiledRenderPipeline compiledRenderPipeline = gpuDevice.precompilePipeline(renderPipeline, cache::getSource);
         if (!compiledRenderPipeline.isValid()) {
            list.add(renderPipeline.getLocation());
         }
      }

      if (!list.isEmpty()) {
         gpuDevice.clearPipelineCache();
         throw new RuntimeException("Failed to load required shader programs:\n" + list.stream().map(id -> " - " + id).collect(Collectors.joining("\n")));
      } else {
         this.cache.close();
         this.cache = cache;
      }
   }

   public String getName() {
      return "Shader Loader";
   }

   private void handleError(Exception exception) {
      if (!this.cache.errorHandled) {
         this.onError.accept(exception);
         this.cache.errorHandled = true;
      }
   }

   @Nullable
   public PostEffectProcessor loadPostEffect(Identifier id, Set<Identifier> availableExternalTargets) {
      try {
         return this.cache.getOrLoadProcessor(id, availableExternalTargets);
      } catch (ShaderLoader.LoadException var4) {
         LOGGER.error("Failed to load post chain: {}", id, var4);
         this.cache.postEffectProcessors.put(id, Optional.empty());
         this.handleError(var4);
         return null;
      }
   }

   @Override
   public void close() {
      this.cache.close();
      this.projectionMatrix.close();
   }

   @Nullable
   public String getSource(Identifier id, ShaderType type) {
      return this.cache.getSource(id, type);
   }

   @Environment(EnvType.CLIENT)
   class Cache implements AutoCloseable {
      private final ShaderLoader.Definitions definitions;
      final Map<Identifier, Optional<PostEffectProcessor>> postEffectProcessors = new HashMap<>();
      boolean errorHandled;

      Cache(final ShaderLoader.Definitions definitions) {
         this.definitions = definitions;
      }

      @Nullable
      public PostEffectProcessor getOrLoadProcessor(Identifier id, Set<Identifier> availableExternalTargets) throws ShaderLoader.LoadException {
         Optional<PostEffectProcessor> optional = this.postEffectProcessors.get(id);
         if (optional != null) {
            return optional.orElse(null);
         } else {
            PostEffectProcessor postEffectProcessor = this.loadProcessor(id, availableExternalTargets);
            this.postEffectProcessors.put(id, Optional.of(postEffectProcessor));
            return postEffectProcessor;
         }
      }

		private PostEffectProcessor loadProcessor(Identifier id, Set<Identifier> availableExternalTargets) throws ShaderLoader.LoadException {
			throw new ShaderLoader.LoadException("Post-processing backport is not wired yet for " + id);
		}

      @Override
      public void close() {
         this.postEffectProcessors.values().forEach(processor -> processor.ifPresent(PostEffectProcessor::close));
         this.postEffectProcessors.clear();
      }

      @Nullable
      public String getSource(Identifier id, ShaderType type) {
         return this.definitions.shaderSources.get(new ShaderLoader.ShaderSourceKey(id, type));
      }
   }

   @Environment(EnvType.CLIENT)
   public record Definitions(Map<ShaderLoader.ShaderSourceKey, String> shaderSources, Map<Identifier, PostEffectPipeline> postChains) {
      public static final ShaderLoader.Definitions EMPTY = new ShaderLoader.Definitions(Map.of(), Map.of());
   }

   @Environment(EnvType.CLIENT)
   public static class LoadException extends Exception {
      public LoadException(String message) {
         super(message);
      }
   }

   @Environment(EnvType.CLIENT)
   record ShaderSourceKey(Identifier id, ShaderType type) {
      @Override
      public String toString() {
         return this.id + " (" + this.type + ")";
      }
   }
}
