#version 460

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelView;
} matrices;

uniform vec3 offset;
uniform vec2 range;

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

#if FOG_TYPE != Linear
const float euler = 2.71828174591064453125;
#endif

void main() {
    vec3 coord = (pos * 0.00439453125 - 16.0) + offset;
    gl_Position = matrices.projection * matrices.modelView * vec4(coord, 1.0);

    fragData.color = color;
    fragData.uv = uv;
    fragData.lightMapUV = (lightMapUV + 8.0) * 0.00390625;

    #if FOG_TYPE == Linear
    fragData.fogAmount = clamp((range.x - length(coord)) * range.y, 0.0, 1.0);
    #elif FOG_TYPE == Exp
    fragData.fogAmount = clamp(pow(euler, -(density * length(coord))), 0.0, 1.0);
    #else
    float exponent = density * length(coord);
    fragData.fogAmount = clamp(pow(euler, -(exponent * exponent)), 0.0, 1.0);
    #endif
}