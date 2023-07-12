#version 460

#include "FogParameters.glsl"

layout(binding = 0) uniform sampler2D blockTexture;
layout(binding = LIGHT_MAP_UNIT) uniform sampler2D lightMapTexture;

in FragData {
    vec4 colorMul;
    vec2 texCoord;
    float lodMultiplier;
    float alphaTestThreshold;
    float fogAmount;
} fragData;

out vec4 fragColor;

void main() {
    fragColor = textureLod(blockTexture, fragData.texCoord, textureQueryLod(blockTexture, fragData.texCoord).y * fragData.lodMultiplier);
    if (fragColor.a <= fragData.alphaTestThreshold) discard;
    fragColor = mix(fogParameters.fogColor, fragColor * fragData.colorMul, fragData.fogAmount);
}