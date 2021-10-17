#version 330
precision mediump float;

uniform mat4 projection;
uniform mat4 modelView;
uniform vec4 defaultColor;

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in int vertColor;

out vec4 color;
out vec2 uv;

const vec4[32] COLORS = vec4[](
    vec4(0.0, 0.0, 0.0, 1.0),
    vec4(0.0, 0.0, 0.6666, 1.0),
    vec4(0.0, 0.6666, 0.0, 1.0),
    vec4(0.0, 0.6666, 0.6666, 1.0),
    vec4(0.6666, 0.0, 0.0, 1.0),
    vec4(0.6666, 0.0, 0.6666, 1.0),
    vec4(1.0, 0.6666, 0.0, 1.0),
    vec4(0.6666, 0.6666, 0.6666, 1.0),
    vec4(0.3333, 0.3333, 0.3333, 1.0),
    vec4(0.3333, 0.3333, 1.0, 1.0),
    vec4(0.3333, 1.0, 0.3333, 1.0),
    vec4(0.3333, 1.0, 1.0, 1.0),
    vec4(1.0, 0.3333, 0.3333, 1.0),
    vec4(1.0, 0.3333, 1.0, 1.0),
    vec4(1.0, 1.0, 0.3333, 1.0),
    vec4(1.0, 1.0, 1.0, 1.0),

    vec4(0.0, 0.0, 0.0, 1.0),
    vec4(0.0, 0.0, 0.1666, 1.0),
    vec4(0.0, 0.1666, 0.0, 1.0),
    vec4(0.0, 0.1666, 0.1666, 1.0),
    vec4(0.1666, 0.0, 0.0, 1.0),
    vec4(0.1666, 0.0, 0.1666, 1.0),
    vec4(0.25, 0.1666, 0.0, 1.0),
    vec4(0.1666, 0.1666, 0.1666, 1.0),
    vec4(0.0833, 0.0833, 0.0833, 1.0),
    vec4(0.0833, 0.0833, 0.25, 1.0),
    vec4(0.0833, 0.25, 0.0833, 1.0),
    vec4(0.0833, 0.25, 0.25, 1.0),
    vec4(0.25, 0.0833, 0.0833, 1.0),
    vec4(0.25, 0.0833, 0.25, 1.0),
    vec4(0.25, 0.25, 0.0833, 1.0),
    vec4(0.25, 0.25, 0.25, 1.0)
);

void main() {
    gl_Position = projection * modelView * vec4(position, 0.0, 1.0);
    uv = vertUV;

    switch(vertColor) {
        case -2:
            color = defaultColor;
            color.rgb *= 0.25;
            break;
        case -1:
            color = defaultColor;
            break;
        default:
            color = COLORS[vertColor];
            color.a *= defaultColor.a;
            break;
    }
}