#version 330

uniform mat4 projection;
uniform mat4 modelView;
uniform vec3 offset;
uniform float density;

layout(location = 0) in vec3 vertPosittion;
layout(location = 1) in vec2 vertLightMapUV;
layout(location = 2) in vec2 vertUV;
layout(location = 3) in vec4 vertColor;

out vec4 color;
out vec2 uv;
out vec2 lightMapUV;
out float fogAmount;

const float euler = 2.71828174591064453125;

void main() {
    vec3 coord = (vertPosittion * 0.00439453125 - 16.0) + offset;
    gl_Position = projection * modelView * vec4(coord, 1.0);
    color = vertColor;
    uv = vertUV;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;

    float exponent = density * length(coord);
    fogAmount = clamp(pow(euler, -(exponent * exponent)), 0.0, 1.0);
}