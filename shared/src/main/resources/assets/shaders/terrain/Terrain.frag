#version 460

layout(std140) uniform FogParameters {
    vec4 fogColor;
    vec2 fogParams;
} fogParameters;

layout(binding = 0) uniform sampler2D blockTexture;
layout(binding = LIGHT_MAP_UNIT) uniform sampler2D lightMapTexture;

in FragData {
    vec4 color;
    vec2 uv;
    vec2 lightMapUV;
    float fogAmount;
    float lodMultiplier;
    float alphaTestThreshold;
} fragData;

out vec4 fragColor;

void main() {
    fragColor = textureLod(blockTexture, fragData.uv, textureQueryLod(blockTexture, fragData.uv).y * fragData.lodMultiplier);
    if (fragColor.a <= fragData.alphaTestThreshold) discard;
    fragColor = mix(fogParameters.fogColor, fragColor * fragData.color * texture(lightMapTexture, fragData.lightMapUV), fragData.fogAmount);
}