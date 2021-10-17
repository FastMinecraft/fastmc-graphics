#version 330
precision mediump float;

uniform sampler2D texture;

in vec4 color;
in vec2 uv;

out vec4 fragColor;

void main() {
    float alpha = texture2D(texture, uv).r;
    if (alpha == 0.0) discard;
    fragColor = color;
    fragColor.a *= alpha;
}