#version 460

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelView;
} matrices;

layout(std140) uniform FogParameters {
    vec3 color;
    vec2 densityRange;
} fogParameters;

uniform vec3 offset;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec2 lightMapUV;
layout(location = 3) in vec3 color;

out FragData {
    vec3 color;
    vec2 uv;
    vec2 lightMapUV;
    float fogAmount;
} fragData;

#if FOG_TYPE != FOG_TYPE_LINEAR
const float euler = 2.71828174591064453125;
#endif

void main() {
    vec3 coord = (pos * 0.003913939 - 0.25) + offset;
    gl_Position = matrices.projection * matrices.modelView * vec4(coord, 1.0);

    fragData.color = color;
    fragData.uv = uv;
    fragData.lightMapUV = (lightMapUV + 8.0) * 0.00390625;

    #if FOG_SHAPE == FOG_SHAPE_SPHERE
    float fogDistance = length(coord);
    #else
    float fogDistance = length(coord.xz);
    #endif

    #if FOG_TYPE == FOG_TYPE_LINEAR
    fragData.fogAmount = clamp((fogParameters.densityRange.x - fogDistance) * fogParameters.densityRange.y, 0.0, 1.0);
    #elif FOG_TYPE == FOG_TYPE_EXP
    fragData.fogAmount = clamp(pow(euler, -(fogParameters.densityRange.x * fogDistance)), 0.0, 1.0);
    #else
    float exponent = fogParameters.densityRange.x * fogDistance;
    fragData.fogAmount = clamp(pow(euler, -(exponent * exponent)), 0.0, 1.0);
    #endif
}