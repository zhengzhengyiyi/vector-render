#version 330

#moj_import <renderer:fog.glsl>
#moj_import <renderer:dynamictransforms.glsl>
#moj_import <renderer:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 sampleLightmap(sampler2D lm, ivec2 uv) {
    vec2 c = clamp((uv / 256.0) + (0.5 / 16.0), vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    return texture(lm, c);
}

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance  = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * sampleLightmap(Sampler2, UV2);
    texCoord0   = UV0;
}
