#version 330

#moj_import <renderer:fog.glsl>
#moj_import <renderer:globals.glsl>
#moj_import <renderer:chunksection.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// Sharp single-texel sample using explicit gradient to control filtering.
vec4 nearestTexel(sampler2D tex, vec2 uv, vec2 px, vec2 du, vec2 dv, vec2 screenPx) {
    vec2 tc = uv / px;
    vec2 center = round(tc) - 0.5;
    vec2 delta  = tc - center;
    delta = (delta - 0.5) * px / screenPx + 0.5;
    delta = clamp(delta, 0.0, 1.0);
    return textureGrad(tex, (center + delta) * px, du, dv);
}

vec4 nearestTexel(sampler2D tex, vec2 uv, vec2 px) {
    vec2 du = dFdx(uv), dv = dFdy(uv);
    vec2 screenPx = sqrt(du * du + dv * dv);
    return nearestTexel(tex, uv, px, du, dv, screenPx);
}

// Rotated-grid super-sampling: 4 sub-pixel samples, two mip levels blended.
vec4 rgssTexel(sampler2D tex, vec2 uv, vec2 px) {
    vec2 du = dFdx(uv), dv = dFdy(uv);
    vec2 screenPx = sqrt(du * du + dv * dv);
    float maxScreen = max(screenPx.x, screenPx.y);
    float minPx = min(px.x, px.y);

    float tStart = minPx * 1.0, tEnd = minPx * 2.0;
    float blend = smoothstep(tStart, tEnd, maxScreen);

    float duLen = length(du), dvLen = length(dv);
    float mipExact = max(0.0, log2(sqrt(min(duLen, dvLen) * max(duLen, dvLen)) / minPx));
    float mipLo = floor(mipExact), mipHi = mipLo + 1.0;
    float mipBlend = fract(mipExact);

    const vec2 jitter[4] = vec2[](
        vec2( 0.125,  0.375),
        vec2(-0.125, -0.375),
        vec2( 0.375, -0.125),
        vec2(-0.375,  0.125)
    );

    vec4 lo = vec4(0.0), hi = vec4(0.0);
    for (int i = 0; i < 4; i++) {
        vec2 s = uv + jitter[i] * px;
        lo += textureLod(tex, s, mipLo);
        hi += textureLod(tex, s, mipHi);
    }
    vec4 rgss = mix(lo * 0.25, hi * 0.25, mipBlend);
    vec4 nearest = nearestTexel(tex, uv, px, du, dv, screenPx);
    return mix(nearest, rgss, blend);
}

void main() {
    vec2 pixelSize = 1.0 / vec2(TextureSize);
    vec4 color = (UseRgss == 1
        ? rgssTexel(Sampler0, texCoord0, pixelSize)
        : nearestTexel(Sampler0, texCoord0, pixelSize)) * vertexColor;

    // Fade in chunks from fog colour.
    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a), color, ChunkVisibility);

#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif

    fragColor = apply_fog(color,
        sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd,
        FogColor);
}
