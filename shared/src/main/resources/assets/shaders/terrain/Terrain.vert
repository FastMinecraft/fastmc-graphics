#version 460

layout(std140) uniform Global {
    mat4 projection;
    mat4 modelView;
    mat4 inverseProjection;
    mat4 inverseModelView;
    vec2 screenResolution;
    float partialTicks;
};

layout(std140) uniform FogParameters {
    vec4 fogColor;
    vec2 fogParams;
} fogParameters;

uniform vec3 regionOffset;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec2 lightMapUV;
layout(location = 3) in vec3 color;
layout(location = 4) in int modelAttribute;

out FragData {
    vec4 color;
    vec2 uv;
    vec2 lightMapUV;
    float fogAmount;
    float lodMultiplier;
    float alphaTestThreshold;
} fragData;

#if FOG_TYPE != FOG_TYPE_LINEAR
const float euler = 2.71828174591064453125;
#endif

const ivec3 shiftVec = ivec3(8, 16, 0);
const ivec3 maskVec = ivec3(255, 65535, 255);
const vec3 coordConvert = vec3(2.51773861295491E-4);

void main() {
    // gl_BaseInstance exploit
    ivec3 chunkOffset = ivec3(gl_BaseInstance) >> shiftVec & maskVec;
    vec3 coord = fma(pos, coordConvert, vec3(chunkOffset) + regionOffset);
    gl_Position = projection * modelView * vec4(coord, 1.0);

    fragData.color = vec4(color, 1.0);
    fragData.uv = uv;
    fragData.lightMapUV = (lightMapUV + 8.0) * 0.00390625;

    fragData.lodMultiplier = float(modelAttribute & 1);
    fragData.alphaTestThreshold = float((modelAttribute >> 1) & 1) - 0.5;

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