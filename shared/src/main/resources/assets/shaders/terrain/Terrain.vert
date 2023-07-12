#version 460

#include "../util/CameraUBO.glsl"
#include "FogParameters.glsl"

layout(binding = LIGHT_MAP_UNIT) uniform sampler2D lightMapTexture;

uniform vec3 regionOffset;

layout(location = 0) in vec3 vPos;
layout(location = 1) in vec2 vTexCoord;
layout(location = 2) in vec2 vLightMapCoord;
layout(location = 3) in vec3 vColorMul;
layout(location = 4) in int vMdlAttribs;

layout(location = 6) in vec3 chunkOffset;

out FragData {
    vec4 colorMul;
    vec2 texCoord;
    float lodMultiplier;
    float alphaTestThreshold;
    float fogAmount;
} fragData;

#if FOG_TYPE != FOG_TYPE_LINEAR
const float euler = 2.71828174591064453125;
#endif

const vec3 coordConvert = vec3(2.51773861295491E-4);

void main() {
    vec3 coord = fma(vPos, coordConvert, chunkOffset * 16.0 + regionOffset);
    gl_Position = projection * modelView * vec4(coord, 1.0);

    vec2 lightMapCoord = (vLightMapCoord + 8.0) * 0.00390625;

    fragData.colorMul = vec4(vColorMul * texture(lightMapTexture, lightMapCoord).rgb, 1.0);
    fragData.texCoord = vTexCoord;

    fragData.lodMultiplier = float(vMdlAttribs & 1);
    fragData.alphaTestThreshold = float((vMdlAttribs >> 1) & 1) - 0.5;

    #if FOG_SHAPE == FOG_SHAPE_SPHERE
    float fogDistance = length(coord);
    #else
    float fogDistance = length(coord.xz);
    #endif

    #if FOG_TYPE == FOG_TYPE_LINEAR
    fragData.fogAmount = clamp(fma(fogParameters.fogParams.x, fogDistance, fogParameters.fogParams.y), 0.0, 1.0);
    #elif FOG_TYPE == FOG_TYPE_EXP
    fragData.fogAmount = pow(euler, -fogParameters.fogParams.x * fogDistance);
    #else
    float exponent = fogParameters.fogParams.x * fogDistance;
    fragData.fogAmount = pow(euler, -(exponent * exponent));
    #endif
}