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

out FragData {
    vec4 color;
    vec2 uv;
    vec2 lightMapUV;
    float fogAmount;
} fragData;

#if FOG_TYPE != FOG_TYPE_LINEAR
const float euler = 2.71828174591064453125;
#endif

const ivec3 shiftVec = ivec3(20, 10, 0);
const vec3 coordConvert = vec3(2.51773861295491E-4);

void main() {
    // gl_BaseInstance exploit
    vec3 coord = fma(pos, coordConvert, ((gl_BaseInstance >> shiftVec) & 1023) + regionOffset);
    gl_Position = projection * modelView * vec4(coord, 1.0);

    fragData.color = vec4(color, 1.0);
    fragData.uv = uv;
    fragData.lightMapUV = lightMapUV * 0.00390625;

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