#version 460

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelView;
};

uniform vec3 offset;
uniform vec2 range;

layout(location = 0) in vec3 vertPosittion;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in vec2 vertLightMapUV;
layout(location = 3) in vec3 vertColor;

out vec3 color;
out vec2 uv;
out vec2 lightMapUV;
out float fogAmount;

void main() {
    vec3 coord = (vertPosittion * 0.00439453125 - 16.0) + offset;
    gl_Position = projection * modelView * vec4(coord, 1.0);
    color = vertColor;
    uv = vertUV;
    lightMapUV = (vertLightMapUV + 8.0) * 0.00390625;

    fogAmount = clamp((range.x - length(coord)) * range.y, 0.0, 1.0);
}